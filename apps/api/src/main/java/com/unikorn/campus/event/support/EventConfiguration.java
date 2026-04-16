package com.unikorn.campus.event.support;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EventProperties.class)
public class EventConfiguration {

    @Bean
    DirectExchange eventExchange(EventProperties properties) {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    Queue eventReserveQueue() {
        return new Queue("campus.event.reserve.queue", true);
    }

    @Bean
    Binding eventReserveBinding(DirectExchange eventExchange, Queue eventReserveQueue, EventProperties properties) {
        return BindingBuilder.bind(eventReserveQueue).to(eventExchange).with(properties.getRoutingKey());
    }
}
