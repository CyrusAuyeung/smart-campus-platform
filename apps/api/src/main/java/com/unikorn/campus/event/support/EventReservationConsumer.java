package com.unikorn.campus.event.support;

import com.unikorn.campus.event.domain.EventReserveCommand;
import com.unikorn.campus.event.domain.EventService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EventReservationConsumer {

    private final EventService eventService;

    public EventReservationConsumer(EventService eventService) {
        this.eventService = eventService;
    }

    @RabbitListener(queues = "campus.event.reserve.queue")
    public void consume(EventReserveCommand command) {
        eventService.confirmReservation(command);
    }
}
