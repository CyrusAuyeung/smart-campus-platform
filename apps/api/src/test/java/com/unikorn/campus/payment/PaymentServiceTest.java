package com.unikorn.campus.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unikorn.campus.booking.api.BookingReceipt;
import com.unikorn.campus.booking.domain.BookingOrderSnapshot;
import com.unikorn.campus.booking.domain.BookingRepository;
import com.unikorn.campus.booking.domain.BookingService;
import com.unikorn.campus.payment.api.BookingPaymentConfirmRequest;
import com.unikorn.campus.payment.domain.BookingTimeoutCommand;
import com.unikorn.campus.payment.domain.PaymentRepository;
import com.unikorn.campus.payment.domain.PaymentService;
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
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingService bookingService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, bookingRepository, bookingService);
    }

    @Test
    void shouldRejectDuplicateTransactionNo() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(paymentRepository.existsTransaction("TX-1")).thenReturn(true);

        assertThatThrownBy(() -> paymentService.confirmBookingPayment(
                orderId,
                new BookingPaymentConfirmRequest(userId, "TX-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("支付流水号已存在");
        verify(bookingService, never()).confirmPayment(orderId, userId);
    }

    @Test
    void shouldConfirmPaymentAndCreateRecord() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BookingReceipt confirmed = new BookingReceipt(
                orderId,
                "ORD-11",
                "CONFIRMED",
                "ACADEMIC",
                "ACADEMIC_SPACE",
                UUID.randomUUID(),
                "预约已支付确认",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                null,
                List.of(),
                List.of(),
                List.of());
        when(paymentRepository.existsTransaction("TX-2")).thenReturn(false);
        when(bookingRepository.findOrderSnapshot(orderId)).thenReturn(Optional.of(new BookingOrderSnapshot(
                orderId,
                userId,
                "ORD-11",
                "PENDING_PAYMENT",
                "ACADEMIC",
                "ACADEMIC_SPACE",
                3L,
                LocalDateTime.now().plusMinutes(5))));
        when(bookingRepository.transitionBookingStatusWithVersion(orderId, "PENDING_PAYMENT", "CONFIRMED", 3L))
                .thenReturn(1);
        when(bookingRepository.findBookingById(orderId)).thenReturn(Optional.of(confirmed));

        BookingReceipt receipt = paymentService.confirmBookingPayment(
                orderId,
                new BookingPaymentConfirmRequest(userId, "TX-2"));

        assertThat(receipt.status()).isEqualTo("CONFIRMED");
        verify(paymentRepository).createPaymentRecord(orderId, userId, "TX-2", "SUCCESS",
                "{\"source\":\"manual-confirm\"}");
        verify(bookingRepository).appendOrderStatusLog(orderId, "PENDING_PAYMENT", "CONFIRMED", "PAYMENT",
                userId.toString());
        verify(bookingService, never()).confirmPayment(orderId, userId);
    }

    @Test
    void shouldMarkPaymentRecordForReviewWhenRaceDetected() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(paymentRepository.existsTransaction("TX-3")).thenReturn(false);
        when(bookingRepository.findOrderSnapshot(orderId)).thenReturn(Optional.of(new BookingOrderSnapshot(
                orderId,
                userId,
                "ORD-12",
                "PENDING_PAYMENT",
                "ACADEMIC",
                "ACADEMIC_SPACE",
                7L,
                LocalDateTime.now().plusMinutes(5))));
        when(bookingRepository.transitionBookingStatusWithVersion(orderId, "PENDING_PAYMENT", "CONFIRMED", 7L))
                .thenReturn(0);

        assertThatThrownBy(() -> paymentService.confirmBookingPayment(
                orderId,
                new BookingPaymentConfirmRequest(userId, "TX-3")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("人工复核");

        verify(paymentRepository).updatePaymentRecordStatus(
                "TX-3",
                "REQUIRES_REVIEW",
                "{\"reason\":\"status-race-detected\"}");
    }

    @Test
    void shouldIgnoreTimeoutWhenOrderAlreadyConfirmed() {
        UUID orderId = UUID.randomUUID();
        when(bookingRepository.findOrderSnapshot(orderId)).thenReturn(Optional.of(new BookingOrderSnapshot(
                orderId,
                UUID.randomUUID(),
                "ORD-10",
                "CONFIRMED",
                "ACADEMIC",
                "ACADEMIC_SPACE",
                9L,
                LocalDateTime.now().plusMinutes(5))));

        paymentService.handleBookingTimeout(new BookingTimeoutCommand(orderId, LocalDateTime.now().minusMinutes(1)));

        verify(bookingService, never()).timeoutBooking(orderId);
    }

    @Test
    void shouldCancelPendingBookingWhenTimeoutArrives() {
        UUID orderId = UUID.randomUUID();
        when(bookingRepository.findOrderSnapshot(orderId)).thenReturn(Optional.of(new BookingOrderSnapshot(
                orderId,
                UUID.randomUUID(),
                "ORD-13",
                "PENDING_PAYMENT",
                "SPORT",
                "SPORT_FACILITY",
                4L,
                LocalDateTime.now().minusMinutes(1))));
        when(bookingRepository.transitionBookingStatusWithVersion(orderId, "PENDING_PAYMENT", "CANCELLED", 4L))
                .thenReturn(1);

        paymentService.handleBookingTimeout(new BookingTimeoutCommand(orderId, LocalDateTime.now().minusMinutes(1)));

        verify(bookingRepository).releaseBookingOccupancy(orderId, "SPORT");
        verify(bookingRepository).appendOrderStatusLog(
                orderId,
                "PENDING_PAYMENT",
                "CANCELLED",
                "TIMEOUT",
                "system-timeout");
        verify(bookingService, never()).timeoutBooking(orderId);
    }
}