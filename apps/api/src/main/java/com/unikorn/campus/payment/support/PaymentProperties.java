package com.unikorn.campus.payment.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    private String exchange = "campus.payment.exchange";
    private String timeoutDelayQueue = "campus.booking.timeout.delay.queue";
    private String timeoutDelayRoutingKey = "campus.booking.timeout.delay";
    private String timeoutRoutingKey = "campus.booking.timeout";
    private String timeoutQueue = "campus.booking.timeout.queue";

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getTimeoutRoutingKey() {
        return timeoutRoutingKey;
    }

    public void setTimeoutRoutingKey(String timeoutRoutingKey) {
        this.timeoutRoutingKey = timeoutRoutingKey;
    }

    public String getTimeoutQueue() {
        return timeoutQueue;
    }

    public void setTimeoutQueue(String timeoutQueue) {
        this.timeoutQueue = timeoutQueue;
    }

    public String getTimeoutDelayQueue() {
        return timeoutDelayQueue;
    }

    public void setTimeoutDelayQueue(String timeoutDelayQueue) {
        this.timeoutDelayQueue = timeoutDelayQueue;
    }

    public String getTimeoutDelayRoutingKey() {
        return timeoutDelayRoutingKey;
    }

    public void setTimeoutDelayRoutingKey(String timeoutDelayRoutingKey) {
        this.timeoutDelayRoutingKey = timeoutDelayRoutingKey;
    }
}
