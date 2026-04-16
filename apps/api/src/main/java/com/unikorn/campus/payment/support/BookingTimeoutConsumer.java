package com.unikorn.campus.payment.support;

import com.unikorn.campus.payment.domain.BookingTimeoutCommand;
import com.unikorn.campus.payment.domain.PaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BookingTimeoutConsumer {

    private final PaymentService paymentService;

    public BookingTimeoutConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = "${app.payment.timeout-queue}")
    public void consume(BookingTimeoutCommand command) {
        paymentService.handleBookingTimeout(command);
    }
}
