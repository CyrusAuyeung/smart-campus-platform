package com.unikorn.campus.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unikorn.campus.booking.api.AcademicBookingRequest;
import com.unikorn.campus.booking.api.BookingReceipt;
import com.unikorn.campus.booking.api.SportBookingRequest;
import com.unikorn.campus.booking.domain.BookingConflictException;
import com.unikorn.campus.booking.domain.BookingOrderSnapshot;
import com.unikorn.campus.booking.domain.BookingRepository;
import com.unikorn.campus.booking.domain.BookingService;
import com.unikorn.campus.booking.domain.BookingTimeoutScheduler;
import com.unikorn.campus.booking.domain.UserCreditSnapshot;
import com.unikorn.campus.booking.domain.UserBookingProfile;
import com.unikorn.campus.booking.support.BookingPolicyProperties;
import com.unikorn.campus.rule.engine.RuleEngine;
import com.unikorn.campus.rule.engine.RuleResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private BookingTimeoutScheduler bookingTimeoutScheduler;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        BookingPolicyProperties properties = new BookingPolicyProperties();
        properties.setAcademicBufferMinutes(5);
        properties.setPaymentTimeoutMinutes(15);
        properties.setSportSlotMinutes(60);
        bookingService = new BookingService(bookingRepository, ruleEngine, properties, bookingTimeoutScheduler);
        when(ruleEngine.evaluateAll(any())).thenReturn(List.of(RuleResult.pass("default", "ok")));
        when(bookingRepository.findUserBookingProfile(any()))
                .thenAnswer(invocation -> Optional.of(new UserBookingProfile(
                        invocation.getArgument(0),
                        "UNDERGRADUATE",
                        100,
                        1)));
        when(bookingRepository.isAcademicSpaceActive(any())).thenReturn(true);
        when(bookingRepository.isSportFacilityActive(any())).thenReturn(true);
        when(bookingRepository.areSportUnitsBelongToFacility(any(), any())).thenReturn(true);
        when(bookingRepository.matchesExistingGroup(any(), any())).thenReturn(true);
        when(bookingRepository.transitionBookingStatus(any(), any(), any())).thenReturn(1);
        when(bookingRepository.transitionBookingStatusWithVersion(any(), any(), any(), any())).thenReturn(1);
        when(bookingRepository.findBookingById(any()))
                .thenReturn(Optional.of(new BookingReceipt(
                        UUID.randomUUID(),
                        "ORD-1",
                        "PENDING_PAYMENT",
                        "ACADEMIC",
                        "ACADEMIC_SPACE",
                        UUID.randomUUID(),
                        "summary",
                        LocalDateTime.now().plusHours(2),
                        LocalDateTime.now().plusHours(3),
                        LocalDateTime.now().plusHours(2).minusMinutes(5),
                        LocalDateTime.now().plusHours(3).plusMinutes(5),
                        null,
                        List.of(),
                        List.of(),
                        List.of()));
        when(bookingRepository.findUserCreditSnapshot(any()))
                .thenAnswer(invocation -> Optional.of(new UserCreditSnapshot(
                        invocation.getArgument(0),
                        100,
                        1)));
        when(bookingRepository.findOrderSnapshot(any()))
                .thenAnswer(invocation -> Optional.of(new BookingOrderSnapshot(
                        invocation.getArgument(0),
                        UUID.randomUUID(),
                        "ORD-X",
                        "PENDING_PAYMENT",
                        "ACADEMIC",
                        "ACADEMIC_SPACE",
                        1L,
                        LocalDateTime.now().plusMinutes(15))));
    }

    @Test
    void shouldCreateAcademicBookingReceipt() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        AcademicBookingRequest request = new AcademicBookingRequest(
                userId,
                spaceId,
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(3));
        when(bookingRepository.hasAcademicConflict(eq(spaceId), any(), any())).thenReturn(false);

        BookingReceipt receipt = bookingService.createAcademicBooking(request);

        assertThat(receipt.businessType()).isEqualTo("ACADEMIC");
        assertThat(receipt.resourceType()).isEqualTo("ACADEMIC_SPACE");
        assertThat(receipt.resourceId()).isEqualTo(spaceId);
        assertThat(receipt.effectiveStartAt()).isEqualTo(request.startAt().minusMinutes(5));
        assertThat(receipt.effectiveEndAt()).isEqualTo(request.endAt().plusMinutes(5));
        verify(bookingRepository).createAcademicBooking(any());
        verify(bookingTimeoutScheduler).scheduleTimeout(eq(receipt.orderId()), any());
    }

    @Test
    void shouldRejectAcademicBookingOnConflict() {
        UUID spaceId = UUID.randomUUID();
        AcademicBookingRequest request = new AcademicBookingRequest(
                UUID.randomUUID(),
                spaceId,
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(3));
        when(bookingRepository.hasAcademicConflict(eq(spaceId), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createAcademicBooking(request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("缓冲期冲突");
        verify(bookingRepository, never()).createAcademicBooking(any());
    }

    @Test
    void shouldCreateSportBookingReceipt() {
        UUID facilityId = UUID.randomUUID();
        List<UUID> unitIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        SportBookingRequest request = new SportBookingRequest(
                UUID.randomUUID(),
                facilityId,
                unitIds,
                LocalDate.now().plusDays(1),
                List.of(2, 3));
        when(bookingRepository.hasSportConflict(unitIds, request.bookingDate(), request.slotIndices()))
                .thenReturn(false);

        BookingReceipt receipt = bookingService.createSportBooking(request);

        assertThat(receipt.businessType()).isEqualTo("SPORT");
        assertThat(receipt.bookingDate()).isEqualTo(request.bookingDate());
        assertThat(receipt.unitIds()).containsExactlyElementsOf(unitIds);
        assertThat(receipt.slotIndices()).containsExactly(2, 3);
        verify(bookingRepository).createSportBooking(any());
        verify(bookingTimeoutScheduler).scheduleTimeout(eq(receipt.orderId()), any());
    }

    @Test
    void shouldRejectSportBookingWhenRuleFails() {
        when(ruleEngine.evaluateAll(any()))
                .thenReturn(List.of(RuleResult.reject("undergraduate-duration", "本科生单次预约时长不能超过 2 小时")));
        SportBookingRequest request = new SportBookingRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(UUID.randomUUID()),
                LocalDate.now().plusDays(1),
                List.of(1, 2, 3));

        assertThatThrownBy(() -> bookingService.createSportBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("本科生单次预约时长不能超过 2 小时");
        verify(bookingRepository, never()).createSportBooking(any());
    }

    @Test
    void shouldRejectAcademicBookingWhenUserMissing() {
        UUID userId = UUID.randomUUID();
        when(bookingRepository.findUserBookingProfile(userId)).thenReturn(Optional.empty());

        AcademicBookingRequest request = new AcademicBookingRequest(
                userId,
                UUID.randomUUID(),
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(3));

        assertThatThrownBy(() -> bookingService.createAcademicBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("预约用户不存在");
    }

    @Test
    void shouldRejectSportBookingWhenGroupTopologyInvalid() {
        UUID facilityId = UUID.randomUUID();
        List<UUID> unitIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(bookingRepository.matchesExistingGroup(facilityId, unitIds)).thenReturn(false);

        SportBookingRequest request = new SportBookingRequest(
                UUID.randomUUID(),
                facilityId,
                unitIds,
                LocalDate.now().plusDays(1),
                List.of(1, 2));

        assertThatThrownBy(() -> bookingService.createSportBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("拓扑分组");
    }

    @Test
    void shouldConfirmPendingBooking() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BookingReceipt pending = new BookingReceipt(
                orderId,
                "ORD-2",
                "PENDING_PAYMENT",
                "ACADEMIC",
                "ACADEMIC_SPACE",
                UUID.randomUUID(),
                "summary",
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(3),
                LocalDateTime.now().plusHours(2).minusMinutes(5),
                LocalDateTime.now().plusHours(3).plusMinutes(5),
                null,
                List.of(),
                List.of(),
                List.of());
        BookingReceipt confirmed = new BookingReceipt(
                orderId,
                "ORD-2",
                "CONFIRMED",
                "ACADEMIC",
                "ACADEMIC_SPACE",
                pending.resourceId(),
                "summary",
                pending.displayStartAt(),
                pending.displayEndAt(),
                pending.effectiveStartAt(),
                pending.effectiveEndAt(),
                null,
                List.of(),
                List.of(),
                List.of());
        when(bookingRepository.findBookingById(orderId)).thenReturn(Optional.of(pending), Optional.of(confirmed));

        BookingReceipt result = bookingService.confirmPayment(orderId, userId);

        assertThat(result.status()).isEqualTo("CONFIRMED");
        verify(bookingRepository).appendOrderStatusLog(orderId, "PENDING_PAYMENT", "CONFIRMED", "PAYMENT",
                userId.toString());
        verify(bookingRepository, never()).releaseBookingOccupancy(any(), any());
    }

    @Test
    void shouldRejectConfirmWhenStatusIsNotPending() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(bookingRepository.findBookingById(orderId)).thenReturn(Optional.of(new BookingReceipt(
                orderId,
                "ORD-3",
                "CONFIRMED",
                "ACADEMIC",
                "ACADEMIC_SPACE",
                UUID.randomUUID(),
                "summary",
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of())));

        assertThatThrownBy(() -> bookingService.confirmPayment(orderId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前订单状态不允许");
    }

    @Test
    void shouldMarkNoShowAndApplyPenalty() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BookingReceipt confirmed = new BookingReceipt(
                orderId,
                "ORD-20",
                "CONFIRMED",
                "ACADEMIC",
                "ACADEMIC_SPACE",
                UUID.randomUUID(),
                "summary",
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(3),
                LocalDateTime.now().plusHours(2).minusMinutes(5),
                LocalDateTime.now().plusHours(3).plusMinutes(5),
                null,
                List.of(),
                List.of(),
                List.of());
        BookingReceipt noShow = new BookingReceipt(
                orderId,
                "ORD-20",
                "NO_SHOW",
                "ACADEMIC",
                "ACADEMIC_SPACE",
                confirmed.resourceId(),
                "summary",
                confirmed.displayStartAt(),
                confirmed.displayEndAt(),
                confirmed.effectiveStartAt(),
                confirmed.effectiveEndAt(),
                null,
                List.of(),
                List.of(),
                List.of());
        when(bookingRepository.findOrderSnapshot(orderId)).thenReturn(Optional.of(new BookingOrderSnapshot(
                orderId,
                userId,
                "ORD-20",
                "CONFIRMED",
                "ACADEMIC",
                "ACADEMIC_SPACE",
                5L,
                LocalDateTime.now().minusMinutes(10))));
        when(bookingRepository.findBookingById(orderId)).thenReturn(Optional.of(confirmed), Optional.of(noShow));

        BookingReceipt result = bookingService.markNoShow(orderId, userId);

        assertThat(result.status()).isEqualTo("NO_SHOW");
        verify(bookingRepository).applyNoShowPenalty(userId, -10);
        verify(bookingRepository).appendOrderStatusLog(orderId, "CONFIRMED", "NO_SHOW", "NO_SHOW", userId.toString());
    }

    @Test
    void shouldPassNoShowRuleContextIntoRuleEngine() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        AcademicBookingRequest request = new AcademicBookingRequest(
                userId,
                spaceId,
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(3));
        when(bookingRepository.hasAcademicConflict(eq(spaceId), any(), any())).thenReturn(false);

        bookingService.createAcademicBooking(request);

        verify(ruleEngine).evaluateAll(argThat(context -> context.recentNoShowCount() == 1));
    }

    @Test
    void shouldReleaseOccupancyWhenCancelPendingBooking() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BookingReceipt pending = new BookingReceipt(
                orderId,
                "ORD-4",
                "PENDING_PAYMENT",
                "SPORT",
                "SPORT_FACILITY",
                UUID.randomUUID(),
                "summary",
                null,
                null,
                null,
                null,
                LocalDate.now().plusDays(1),
                List.of(1, 2),
                List.of(UUID.randomUUID()),
                List.of());
        BookingReceipt cancelled = new BookingReceipt(
                orderId,
                "ORD-4",
                "CANCELLED",
                "SPORT",
                "SPORT_FACILITY",
                pending.resourceId(),
                "summary",
                null,
                null,
                null,
                null,
                pending.bookingDate(),
                pending.slotIndices(),
                pending.unitIds(),
                List.of());
        when(bookingRepository.findBookingById(orderId)).thenReturn(Optional.of(pending), Optional.of(cancelled));

        BookingReceipt result = bookingService.cancelBooking(orderId, userId);

        assertThat(result.status()).isEqualTo("CANCELLED");
        verify(bookingRepository).releaseBookingOccupancy(orderId, "SPORT");
        verify(bookingRepository).appendOrderStatusLog(orderId, "PENDING_PAYMENT", "CANCELLED", "USER_CANCEL",
                userId.toString());
    }
}
