package com.unikorn.campus.catalog.api;

public record ResourceItem(
        String id,
        String code,
        String type,
        String name,
        String campus,
        String building,
        int capacity,
        String status) {
}
