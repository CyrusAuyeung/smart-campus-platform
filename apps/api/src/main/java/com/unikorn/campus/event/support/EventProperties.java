package com.unikorn.campus.event.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.event")
public class EventProperties {

    private String stockKeyPrefix = "event:stock:";
    private String userKeyPrefix = "event:user:";
    private String exchange = "campus.event.exchange";
    private String routingKey = "campus.event.reserve";

    public String getStockKeyPrefix() {
        return stockKeyPrefix;
    }

    public void setStockKeyPrefix(String stockKeyPrefix) {
        this.stockKeyPrefix = stockKeyPrefix;
    }

    public String getUserKeyPrefix() {
        return userKeyPrefix;
    }

    public void setUserKeyPrefix(String userKeyPrefix) {
        this.userKeyPrefix = userKeyPrefix;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
}
