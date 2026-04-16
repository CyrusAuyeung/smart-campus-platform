package com.unikorn.campus.payment.api;

import com.unikorn.campus.booking.api.BookingReceipt;
import com.unikorn.campus.payment.domain.PaymentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/bookings/{orderId}/confirm")
    public BookingReceipt confirmBookingPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody BookingPaymentConfirmRequest request) {
        return paymentService.confirmBookingPayment(orderId, request);
    }
}
