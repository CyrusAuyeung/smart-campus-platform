package com.unikorn.campus.event.domain;

public class EventSoldOutException extends RuntimeException {

    public EventSoldOutException(String message) {
        super(message);
    }
}
