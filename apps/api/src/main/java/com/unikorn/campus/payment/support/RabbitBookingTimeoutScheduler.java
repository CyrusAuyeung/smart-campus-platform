package com.unikorn.campus.payment.support;

import com.unikorn.campus.booking.domain.BookingTimeoutScheduler;
import com.unikorn.campus.payment.domain.BookingTimeoutCommand;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitBookingTimeoutScheduler implements BookingTimeoutScheduler {

    private final RabbitTemplate rabbitTemplate;
    private final PaymentProperties paymentProperties;

    public RabbitBookingTimeoutScheduler(RabbitTemplate rabbitTemplate, PaymentProperties paymentProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.paymentProperties = paymentProperties;
    }

    @Override
    public void scheduleTimeout(UUID orderId, LocalDateTime paymentDeadline) {
        long delay = Math.max(Duration.between(LocalDateTime.now(), paymentDeadline).toMillis(), 0L);
        BookingTimeoutCommand command = new BookingTimeoutCommand(orderId, paymentDeadline);
        MessagePostProcessor ttlProcessor = (Message message) -> {
            message.getMessageProperties().setExpiration(String.valueOf(delay));
            return message;
        };
        rabbitTemplate.convertAndSend(
                paymentProperties.getExchange(),
                paymentProperties.getTimeoutDelayRoutingKey(),
                command,
                ttlProcessor);
    }
}
