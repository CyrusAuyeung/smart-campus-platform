package com.unikorn.campus.event.api;

import com.unikorn.campus.event.domain.EventSoldOutException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class EventExceptionHandler {

    @ExceptionHandler(EventSoldOutException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleSoldOut(EventSoldOutException exception) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.CONFLICT.value(),
                "error", "EVENT_SOLD_OUT",
                "message", exception.getMessage());
    }
}
