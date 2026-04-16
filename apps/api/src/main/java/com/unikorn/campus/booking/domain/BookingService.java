package com.unikorn.campus.booking.domain;

import com.unikorn.campus.booking.api.AcademicBookingRequest;
import com.unikorn.campus.booking.api.BookingReceipt;
import com.unikorn.campus.booking.api.SportBookingRequest;
import com.unikorn.campus.booking.support.BookingPolicyProperties;
import com.unikorn.campus.rule.engine.RuleContext;
import com.unikorn.campus.rule.engine.RuleEngine;
import com.unikorn.campus.rule.engine.RuleResult;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RuleEngine ruleEngine;
    private final BookingPolicyProperties bookingPolicyProperties;
    private final BookingTimeoutScheduler bookingTimeoutScheduler;

    public BookingService(
            BookingRepository bookingRepository,
            RuleEngine ruleEngine,
            BookingPolicyProperties bookingPolicyProperties,
            BookingTimeoutScheduler bookingTimeoutScheduler) {
        this.bookingRepository = bookingRepository;
        this.ruleEngine = ruleEngine;
        this.bookingPolicyProperties = bookingPolicyProperties;
        this.bookingTimeoutScheduler = bookingTimeoutScheduler;
    }

    @Transactional
    public BookingReceipt createAcademicBooking(AcademicBookingRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("预约结束时间必须晚于开始时间");
        }

        UserBookingProfile profile = loadUserProfile(request.userId());
        ensureAcademicSpaceActive(request.spaceId());

        List<RuleResult> ruleResults = evaluateRules(
                profile.roleCode(),
                profile.creditScore(),
                "ACADEMIC_SPACE",
                Duration.between(request.startAt(), request.endAt()),
                profile.recentNoShowCount());
        ensureAllowed(ruleResults);

        LocalDateTime effectiveStartAt = request.startAt()
                .minusMinutes(bookingPolicyProperties.getAcademicBufferMinutes());
        LocalDateTime effectiveEndAt = request.endAt().plusMinutes(bookingPolicyProperties.getAcademicBufferMinutes());

        if (bookingRepository.hasAcademicConflict(request.spaceId(), effectiveStartAt, effectiveEndAt)) {
            throw new BookingConflictException("当前学术空间在所选时间段内已被占用或与缓冲期冲突");
        }

        UUID orderId = UUID.randomUUID();
        String orderNo = generateOrderNo("AC");
        LocalDateTime paymentDeadline = LocalDateTime.now()
                .plusMinutes(bookingPolicyProperties.getPaymentTimeoutMinutes());
        AcademicBookingCommand command = AcademicBookingCommand.of(
                orderId,
                orderNo,
                request,
                effectiveStartAt,
                effectiveEndAt,
                paymentDeadline);
        bookingRepository.createAcademicBooking(command);
        bookingTimeoutScheduler.scheduleTimeout(orderId, paymentDeadline);

        return new BookingReceipt(
                orderId,
                orderNo,
                "PENDING_PAYMENT",
                "ACADEMIC",
                "ACADEMIC_SPACE",
                request.spaceId(),
                "学术空间预约已创建，系统已自动锁定前后缓冲时间",
                request.startAt(),
                request.endAt(),
                effectiveStartAt,
                effectiveEndAt,
                null,
                List.of(),
                List.of(),
                ruleResults);
    }

    @Transactional
    public BookingReceipt createSportBooking(SportBookingRequest request) {
        if (request.slotIndices().stream().distinct().count() != request.slotIndices().size()) {
            throw new IllegalArgumentException("同一笔体育预约不能包含重复槽位");
        }

        UserBookingProfile profile = loadUserProfile(request.userId());
        ensureSportFacilityActive(request.facilityId());
        ensureSportUnitsBelongToFacility(request.facilityId(), request.unitIds());
        ensureCombinedUnitGroup(request.facilityId(), request.unitIds());

        List<RuleResult> ruleResults = evaluateRules(
                profile.roleCode(),
                profile.creditScore(),
                "SPORT_FACILITY",
                Duration.ofMinutes((long) bookingPolicyProperties.getSportSlotMinutes() * request.slotIndices().size()),
                profile.recentNoShowCount());
        ensureAllowed(ruleResults);

        if (bookingRepository.hasSportConflict(request.unitIds(), request.bookingDate(), request.slotIndices())) {
            throw new BookingConflictException("当前体育设施单元在所选槽位中已有占用");
        }

        UUID orderId = UUID.randomUUID();
        String orderNo = generateOrderNo("SP");
        LocalDateTime paymentDeadline = LocalDateTime.now()
                .plusMinutes(bookingPolicyProperties.getPaymentTimeoutMinutes());
        SportBookingCommand command = SportBookingCommand.of(orderId, orderNo, request, paymentDeadline);
        bookingRepository.createSportBooking(command);
        bookingTimeoutScheduler.scheduleTimeout(orderId, paymentDeadline);

        return new BookingReceipt(
                orderId,
                orderNo,
                "PENDING_PAYMENT",
                "SPORT",
                "SPORT_FACILITY",
                request.facilityId(),
                "体育设施预约已创建，已按离散槽位锁定场地单元",
                null,
                null,
                null,
                null,
                request.bookingDate(),
                List.copyOf(request.slotIndices()),
                List.copyOf(request.unitIds()),
                ruleResults);
    }

    @Transactional(readOnly = true)
    public List<BookingReceipt> listBookings(UUID userId) {
        loadUserProfile(userId);
        return bookingRepository.findBookingsByUserId(userId);
    }

    @Transactional
    public BookingReceipt confirmPayment(UUID orderId, UUID userId) {
        loadUserProfile(userId);
        return transitionStatus(orderId, "PENDING_PAYMENT", "CONFIRMED", "PAYMENT", userId.toString(), "预约已支付确认");
    }

    @Transactional
    public BookingReceipt cancelBooking(UUID orderId, UUID userId) {
        loadUserProfile(userId);
        return transitionStatus(orderId, "PENDING_PAYMENT", "CANCELLED", "USER_CANCEL", userId.toString(), "预约已取消");
    }

    @Transactional
    public BookingReceipt timeoutBooking(UUID orderId) {
        return transitionStatus(orderId, "PENDING_PAYMENT", "CANCELLED", "TIMEOUT", "system-timeout", "预约已超时取消");
    }

    @Transactional
    public BookingReceipt markNoShow(UUID orderId, UUID userId) {
        UserCreditSnapshot creditSnapshot = bookingRepository.findUserCreditSnapshot(userId)
                .orElseThrow(() -> new IllegalArgumentException("预约用户不存在或不可用"));
        BookingReceipt receipt = transitionStatus(
                orderId,
                "CONFIRMED",
                "NO_SHOW",
                "NO_SHOW",
                userId.toString(),
                "预约已标记为爽约，信用分已扣减");
        bookingRepository.applyNoShowPenalty(creditSnapshot.userId(), -10);
        return receipt;
    }

    private List<RuleResult> evaluateRules(
            String userRole,
            int creditScore,
            String resourceType,
            Duration requestedDuration,
            int recentNoShowCount) {
        return ruleEngine.evaluateAll(
                new RuleContext(userRole, creditScore, resourceType, requestedDuration, recentNoShowCount));
    }

    private void ensureAllowed(List<RuleResult> ruleResults) {
        ruleResults.stream()
                .filter(result -> !result.allowed())
                .findFirst()
                .ifPresent(result -> {
                    throw new IllegalArgumentException(result.message());
                });
    }

    private UserBookingProfile loadUserProfile(UUID userId) {
        return bookingRepository.findUserBookingProfile(userId)
                .orElseThrow(() -> new IllegalArgumentException("预约用户不存在或不可用"));
    }

    private void ensureAcademicSpaceActive(UUID spaceId) {
        if (!bookingRepository.isAcademicSpaceActive(spaceId)) {
            throw new IllegalArgumentException("学术空间不存在或当前不可预约");
        }
    }

    private void ensureSportFacilityActive(UUID facilityId) {
        if (!bookingRepository.isSportFacilityActive(facilityId)) {
            throw new IllegalArgumentException("体育设施不存在或当前不可预约");
        }
    }

    private void ensureSportUnitsBelongToFacility(UUID facilityId, List<UUID> unitIds) {
        if (!bookingRepository.areSportUnitsBelongToFacility(facilityId, unitIds)) {
            throw new IllegalArgumentException("所选场地单元与当前体育设施不匹配");
        }
    }

    private void ensureCombinedUnitGroup(UUID facilityId, List<UUID> unitIds) {
        if (unitIds.size() <= 1) {
            return;
        }

        if (!bookingRepository.matchesExistingGroup(facilityId, unitIds)) {
            throw new IllegalArgumentException("当前组合场地不符合已定义的拓扑分组");
        }
    }

    private String generateOrderNo(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }

    private BookingReceipt transitionStatus(
            UUID orderId,
            String expectedStatus,
            String newStatus,
            String triggerType,
            String triggerBy,
            String summary) {
        BookingOrderSnapshot order = bookingRepository.findOrderSnapshot(orderId)
                .orElseThrow(() -> new IllegalArgumentException("预约订单不存在"));
        BookingReceipt current = bookingRepository.findBookingById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("预约订单不存在"));

        if (!expectedStatus.equals(current.status())) {
            throw new IllegalArgumentException("当前订单状态不允许执行该操作");
        }

        int updated = bookingRepository.transitionBookingStatusWithVersion(
                orderId,
                expectedStatus,
                newStatus,
                order.version());
        if (updated == 0) {
            throw new IllegalStateException("订单状态更新失败，请重试");
        }

        if ("CANCELLED".equals(newStatus)) {
            bookingRepository.releaseBookingOccupancy(orderId, current.businessType());
        }

        bookingRepository.appendOrderStatusLog(orderId, expectedStatus, newStatus, triggerType, triggerBy);
        BookingReceipt updatedReceipt = bookingRepository.findBookingById(orderId)
                .orElseThrow(() -> new IllegalStateException("订单状态更新后无法读取"));

        return new BookingReceipt(
                updatedReceipt.orderId(),
                updatedReceipt.orderNo(),
                updatedReceipt.status(),
                updatedReceipt.businessType(),
                updatedReceipt.resourceType(),
                updatedReceipt.resourceId(),
                summary,
                updatedReceipt.displayStartAt(),
                updatedReceipt.displayEndAt(),
                updatedReceipt.effectiveStartAt(),
                updatedReceipt.effectiveEndAt(),
                updatedReceipt.bookingDate(),
                updatedReceipt.slotIndices(),
                updatedReceipt.unitIds(),
                updatedReceipt.ruleResults());
    }
}
