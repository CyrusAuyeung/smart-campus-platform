package com.unikorn.campus.event.domain;

import com.unikorn.campus.event.api.EventItem;
import com.unikorn.campus.event.api.EventHealthSnapshot;
import com.unikorn.campus.event.api.EventRepairAction;
import com.unikorn.campus.event.api.EventReserveReceipt;
import com.unikorn.campus.event.api.EventReserveRequest;
import com.unikorn.campus.event.api.EventReconciliationSnapshot;
import com.unikorn.campus.event.support.EventProperties;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>(
            "local stock = tonumber(redis.call('GET', KEYS[1]) or '-1') " +
                    "if stock <= 0 then return -1 end " +
                    "if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then return -2 end " +
                    "redis.call('DECR', KEYS[1]) " +
                    "redis.call('SADD', KEYS[2], ARGV[1]) " +
                    "return stock - 1",
            Long.class);

    private final EventRepository eventRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final EventProperties eventProperties;

    public EventService(
            EventRepository eventRepository,
            StringRedisTemplate stringRedisTemplate,
            RabbitTemplate rabbitTemplate,
            EventProperties eventProperties) {
        this.eventRepository = eventRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.eventProperties = eventProperties;
    }

    public List<EventItem> listEvents() {
        List<EventItem> events = eventRepository.findPublishedEvents();
        events.stream()
                .filter(event -> event.id() != null && !event.id().isBlank())
                .forEach(this::warmStockIfNecessary);
        return events;
    }

    public EventReserveReceipt reserve(EventReserveRequest request) {
        EventStockSnapshot snapshot = eventRepository.findEventStock(request.eventId())
                .orElseThrow(() -> new IllegalArgumentException("活动不存在或当前不可预约"));
        if (!"PUBLISHED".equalsIgnoreCase(snapshot.status())) {
            throw new IllegalArgumentException("活动当前不可抢票");
        }
        if (eventRepository.hasExistingReservation(request.eventId(), request.userId())) {
            throw new IllegalArgumentException("当前用户已经抢过该活动");
        }

        warmStockIfNecessary(new EventItem(
                snapshot.eventId().toString(),
                "",
                snapshot.title(),
                snapshot.status(),
                null,
                null,
                snapshot.availableStock(),
                snapshot.availableStock(),
                snapshot.limitPerUser()));

        String stockKey = stockKey(request.eventId());
        String userKey = userKey(request.eventId());
        Long remaining = stringRedisTemplate.execute(
                RESERVE_SCRIPT,
                List.of(stockKey, userKey),
                request.userId().toString());

        if (remaining == null) {
            throw new IllegalStateException("活动库存脚本执行失败");
        }
        if (remaining == -1L) {
            throw new EventSoldOutException("活动名额已抢完");
        }
        if (remaining == -2L) {
            throw new IllegalArgumentException("当前用户已经抢过该活动");
        }

        String requestId = UUID.randomUUID().toString();
        EventReserveCommand command = new EventReserveCommand(
                UUID.randomUUID(),
                "EV-" + System.currentTimeMillis(),
                request.eventId(),
                request.userId(),
                requestId);
        rabbitTemplate.convertAndSend(eventProperties.getExchange(), eventProperties.getRoutingKey(), command);

        return new EventReserveReceipt(requestId, "QUEUED", "抢票请求已进入队列，等待异步确认", remaining.intValue());
    }

    public void confirmReservation(EventReserveCommand command) {
        if (eventRepository.hasProcessedRequest(command.requestId())) {
            return;
        }

        try {
            eventRepository.createPendingReservation(
                    command.orderId(),
                    command.orderNo(),
                    command.eventId(),
                    command.userId(),
                    command.requestId());
            eventRepository.decreasePersistentStock(command.eventId());
            eventRepository.markReservationConfirmed(command.requestId());
        } catch (RuntimeException exception) {
            rollbackReservation(command, exception.getMessage());
            throw exception;
        }
    }

    public List<EventReservationAudit> listReservationAudits() {
        return eventRepository.findReservationAudits();
    }

    public EventHealthSnapshot loadHealthSnapshot() {
        return eventRepository.loadHealthSnapshot();
    }

    public List<EventReconciliationSnapshot> loadReconciliationSnapshots() {
        return eventRepository.loadReconciliationSnapshots().stream()
                .map(snapshot -> {
                    String cached = stringRedisTemplate.opsForValue()
                            .get(stockKey(UUID.fromString(snapshot.eventId())));
                    int cacheAvailable = cached != null ? Integer.parseInt(cached) : snapshot.databaseAvailableStock();
                    return new EventReconciliationSnapshot(
                            snapshot.eventId(),
                            snapshot.databaseAvailableStock(),
                            cacheAvailable,
                            snapshot.databaseAvailableStock() == cacheAvailable);
                })
                .toList();
    }

    public EventReconciliationSnapshot reconcileEventStock(UUID eventId) {
        int databaseStock = eventRepository.findDatabaseAvailableStock(eventId)
                .orElseThrow(() -> new IllegalArgumentException("活动不存在"));
        String currentCache = stringRedisTemplate.opsForValue().get(stockKey(eventId));
        Integer previousCache = currentCache != null ? Integer.parseInt(currentCache) : null;
        stringRedisTemplate.opsForValue().set(stockKey(eventId), String.valueOf(databaseStock));
        eventRepository.createRepairAction(eventId, "STOCK_RECONCILIATION", previousCache, databaseStock,
                "system-reconcile");
        return new EventReconciliationSnapshot(eventId.toString(), databaseStock, databaseStock, true);
    }

    public List<EventRepairAction> listRepairActions() {
        return eventRepository.findRepairActions();
    }

    private void warmStockIfNecessary(EventItem event) {
        String stockKey = stockKey(UUID.fromString(event.id()));
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
            return;
        }

        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(event.availableStock()), Duration.ofHours(4));
        stringRedisTemplate.opsForSet().getOperations().expire(userKey(UUID.fromString(event.id())),
                Duration.ofHours(4));
    }

    private String stockKey(UUID eventId) {
        return eventProperties.getStockKeyPrefix() + eventId;
    }

    private String userKey(UUID eventId) {
        return eventProperties.getUserKeyPrefix() + eventId;
    }

    private void rollbackReservation(EventReserveCommand command, String reason) {
        stringRedisTemplate.opsForValue().increment(stockKey(command.eventId()));
        stringRedisTemplate.opsForSet().remove(userKey(command.eventId()), command.userId().toString());
        eventRepository.increasePersistentStock(command.eventId());
        eventRepository.markReservationFailed(command.requestId(), reason);
    }
}
