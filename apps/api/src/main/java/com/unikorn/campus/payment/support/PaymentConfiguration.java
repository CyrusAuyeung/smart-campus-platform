package com.unikorn.campus.payment.support;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentConfiguration {

    @Bean
    DirectExchange paymentExchange(PaymentProperties properties) {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    Queue bookingTimeoutQueue(PaymentProperties properties) {
        return new Queue(properties.getTimeoutQueue(), true);
    }

    @Bean
    Queue bookingTimeoutDelayQueue(PaymentProperties properties) {
        return new Queue(
                properties.getTimeoutDelayQueue(),
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", properties.getExchange(),
                        "x-dead-letter-routing-key", properties.getTimeoutRoutingKey()));
    }

    @Bean
    Binding bookingTimeoutBinding(
            DirectExchange paymentExchange,
            Queue bookingTimeoutQueue,
            PaymentProperties properties) {
        return BindingBuilder.bind(bookingTimeoutQueue)
                .to(paymentExchange)
                .with(properties.getTimeoutRoutingKey());
    }

    @Bean
    Binding bookingTimeoutDelayBinding(
            DirectExchange paymentExchange,
            Queue bookingTimeoutDelayQueue,
            PaymentProperties properties) {
        return BindingBuilder.bind(bookingTimeoutDelayQueue)
                .to(paymentExchange)
                .with(properties.getTimeoutDelayRoutingKey());
    }
}
