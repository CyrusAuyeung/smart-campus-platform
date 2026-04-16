package com.unikorn.campus.payment.domain;

import com.unikorn.campus.booking.api.BookingReceipt;
import com.unikorn.campus.booking.domain.BookingOrderSnapshot;
import com.unikorn.campus.booking.domain.BookingRepository;
import com.unikorn.campus.booking.domain.BookingService;
import com.unikorn.campus.payment.api.BookingPaymentConfirmRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            BookingService bookingService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    @Transactional
    public BookingReceipt confirmBookingPayment(UUID orderId, BookingPaymentConfirmRequest request) {
        if (paymentRepository.existsTransaction(request.transactionNo())) {
            throw new IllegalArgumentException("支付流水号已存在");
        }

        BookingOrderSnapshot snapshot = bookingRepository.findOrderSnapshot(orderId)
                .orElseThrow(() -> new IllegalArgumentException("预约订单不存在"));
        if (!snapshot.userId().equals(request.userId())) {
            throw new IllegalArgumentException("支付用户与订单用户不匹配");
        }
        if (!"PENDING_PAYMENT".equals(snapshot.status())) {
            throw new IllegalArgumentException("当前订单状态不允许支付确认");
        }

        paymentRepository.createPaymentRecord(
                orderId,
                request.userId(),
                request.transactionNo(),
                "SUCCESS",
                "{\"source\":\"manual-confirm\"}");

        int updated = bookingRepository.transitionBookingStatusWithVersion(
                orderId,
                "PENDING_PAYMENT",
                "CONFIRMED",
                snapshot.version());
        if (updated == 0) {
            paymentRepository.updatePaymentRecordStatus(
                    request.transactionNo(),
                    "REQUIRES_REVIEW",
                    "{\"reason\":\"status-race-detected\"}");
            throw new IllegalStateException("支付确认与超时取消发生竞争，请进入人工复核");
        }

        bookingRepository.appendOrderStatusLog(orderId, "PENDING_PAYMENT", "CONFIRMED", "PAYMENT",
                request.userId().toString());
        return bookingRepository.findBookingById(orderId)
                .map(receipt -> new BookingReceipt(
                        receipt.orderId(),
                        receipt.orderNo(),
                        receipt.status(),
                        receipt.businessType(),
                        receipt.resourceType(),
                        receipt.resourceId(),
                        "预约已支付确认",
                        receipt.displayStartAt(),
                        receipt.displayEndAt(),
                        receipt.effectiveStartAt(),
                        receipt.effectiveEndAt(),
                        receipt.bookingDate(),
                        receipt.slotIndices(),
                        receipt.unitIds(),
                        receipt.ruleResults()))
                .orElseThrow(() -> new IllegalStateException("支付确认后无法读取订单回执"));
    }

    @Transactional
    public void handleBookingTimeout(BookingTimeoutCommand command) {
        bookingRepository.findOrderSnapshot(command.orderId()).ifPresent(order -> {
            if (!"PENDING_PAYMENT".equals(order.status())) {
                return;
            }
            if (command.paymentDeadline().isAfter(LocalDateTime.now())) {
                return;
            }

            int updated = bookingRepository.transitionBookingStatusWithVersion(
                    command.orderId(),
                    "PENDING_PAYMENT",
                    "CANCELLED",
                    order.version());
            if (updated == 0) {
                return;
            }

            bookingRepository.releaseBookingOccupancy(command.orderId(), order.businessType());
            bookingRepository.appendOrderStatusLog(
                    command.orderId(),
                    "PENDING_PAYMENT",
                    "CANCELLED",
                    "TIMEOUT",
                    "system-timeout");
        });
    }
}
