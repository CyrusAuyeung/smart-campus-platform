package com.unikorn.campus.catalog.api;

public record UserItem(
        String id,
        String studentNo,
        String displayName,
        String roleCode,
        int creditScore,
        int recentNoShowCount) {
}
