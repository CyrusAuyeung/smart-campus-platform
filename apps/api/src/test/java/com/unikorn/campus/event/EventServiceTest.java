package com.unikorn.campus.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unikorn.campus.event.api.EventItem;
import com.unikorn.campus.event.api.EventHealthSnapshot;
import com.unikorn.campus.event.api.EventReconciliationSnapshot;
import com.unikorn.campus.event.api.EventReserveReceipt;
import com.unikorn.campus.event.api.EventReserveRequest;
import com.unikorn.campus.event.domain.EventRepository;
import com.unikorn.campus.event.domain.EventReservationAudit;
import com.unikorn.campus.event.domain.EventReserveCommand;
import com.unikorn.campus.event.domain.EventService;
import com.unikorn.campus.event.domain.EventSoldOutException;
import com.unikorn.campus.event.domain.EventStockSnapshot;
import com.unikorn.campus.event.support.EventProperties;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        EventProperties properties = new EventProperties();
        eventService = new EventService(eventRepository, stringRedisTemplate, rabbitTemplate, properties);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldQueueReservationWhenStockAvailable() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(eventRepository.findEventStock(eventId))
                .thenReturn(Optional.of(new EventStockSnapshot(eventId, "测试活动", 20, 1, "PUBLISHED")));
        when(eventRepository.hasExistingReservation(eventId, userId)).thenReturn(false);
        when(stringRedisTemplate.hasKey(any())).thenReturn(true);
        when(stringRedisTemplate.execute(any(), any(), eq(userId.toString()))).thenReturn(19L);

        EventReserveReceipt receipt = eventService.reserve(new EventReserveRequest(userId, eventId));

        assertThat(receipt.status()).isEqualTo("QUEUED");
        assertThat(receipt.remainingStock()).isEqualTo(19);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void shouldRejectWhenSoldOut() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(eventRepository.findEventStock(eventId))
                .thenReturn(Optional.of(new EventStockSnapshot(eventId, "测试活动", 0, 1, "PUBLISHED")));
        when(eventRepository.hasExistingReservation(eventId, userId)).thenReturn(false);
        when(stringRedisTemplate.hasKey(any())).thenReturn(true);
        when(stringRedisTemplate.execute(any(), any(), eq(userId.toString()))).thenReturn(-1L);

        assertThatThrownBy(() -> eventService.reserve(new EventReserveRequest(userId, eventId)))
                .isInstanceOf(EventSoldOutException.class)
                .hasMessageContaining("已抢完");
    }

        @Test
        void shouldRollbackRedisReservationWhenPublishFails() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(eventRepository.findEventStock(eventId))
            .thenReturn(Optional.of(new EventStockSnapshot(eventId, "测试活动", 20, 1, "PUBLISHED")));
        when(eventRepository.hasExistingReservation(eventId, userId)).thenReturn(false);
        when(stringRedisTemplate.hasKey(any())).thenReturn(true);
        when(stringRedisTemplate.execute(any(), any(), eq(userId.toString()))).thenReturn(19L);
        doThrow(new IllegalStateException("mq failed"))
            .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertThatThrownBy(() -> eventService.reserve(new EventReserveRequest(userId, eventId)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("入队失败");

        verify(stringRedisTemplate.opsForValue()).increment(any(String.class));
        verify(stringRedisTemplate.opsForSet()).remove(any(String.class), eq(userId.toString()));
        }

    @Test
    void shouldListPublishedEvents() {
        when(eventRepository.findPublishedEvents()).thenReturn(List.of(
                new EventItem(UUID.randomUUID().toString(), "E-1", "测试活动", "PUBLISHED", null, null, 100, 88, 1)));
        when(stringRedisTemplate.hasKey(any())).thenReturn(true);

        List<EventItem> events = eventService.listEvents();

        assertThat(events).hasSize(1);
    }

    @Test
    void shouldRollbackReservationWhenPersistentStockDecreaseFails() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EventReserveCommand command = new EventReserveCommand(UUID.randomUUID(), "EV-1", eventId, userId, "REQ-1");
        when(eventRepository.hasProcessedRequest("REQ-1")).thenReturn(false);
        doThrow(new IllegalStateException("db failed"))
            .when(eventRepository).createPendingReservation(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> eventService.confirmReservation(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db failed");

        verify(stringRedisTemplate.opsForValue()).increment(any(String.class));
        verify(stringRedisTemplate.opsForSet()).remove(any(String.class), eq(userId.toString()));
        verify(eventRepository).increasePersistentStock(eventId);
        verify(eventRepository).markReservationFailed("REQ-1", "db failed");
    }

    @Test
    void shouldIgnoreDuplicateReservationMessage() {
        EventReserveCommand command = new EventReserveCommand(UUID.randomUUID(), "EV-2", UUID.randomUUID(),
                UUID.randomUUID(), "REQ-2");
        when(eventRepository.hasProcessedRequest("REQ-2")).thenReturn(true);

        eventService.confirmReservation(command);

        verify(eventRepository).hasProcessedRequest("REQ-2");
    }

    @Test
    void shouldExposeHealthSnapshot() {
        when(eventRepository.loadHealthSnapshot()).thenReturn(new EventHealthSnapshot(1, 2, 3));

        EventHealthSnapshot snapshot = eventService.loadHealthSnapshot();

        assertThat(snapshot.failedOrders()).isEqualTo(3);
    }

    @Test
    void shouldExposeReservationAudits() {
        when(eventRepository.findReservationAudits()).thenReturn(List.of(
                new EventReservationAudit("REQ-2", UUID.randomUUID(), UUID.randomUUID(), "CONFIRMED", null,
                        java.time.LocalDateTime.now())));

        List<EventReservationAudit> audits = eventService.listReservationAudits();

        assertThat(audits).hasSize(1);
    }

    @Test
    void shouldReconcileStockUsingDatabaseValue() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findDatabaseAvailableStock(eventId)).thenReturn(Optional.of(66));

        EventReconciliationSnapshot snapshot = eventService.reconcileEventStock(eventId);

        assertThat(snapshot.consistent()).isTrue();
        assertThat(snapshot.databaseAvailableStock()).isEqualTo(66);
        verify(stringRedisTemplate.opsForValue()).set(any(String.class), eq("66"));
    }
}
