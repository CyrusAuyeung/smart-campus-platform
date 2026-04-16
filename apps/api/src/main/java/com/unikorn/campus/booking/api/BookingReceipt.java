package com.unikorn.campus.booking.api;

import com.unikorn.campus.rule.engine.RuleResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookingReceipt(
        UUID orderId,
        String orderNo,
        String status,
        String businessType,
        String resourceType,
        UUID resourceId,
        String summary,
        LocalDateTime displayStartAt,
        LocalDateTime displayEndAt,
        LocalDateTime effectiveStartAt,
        LocalDateTime effectiveEndAt,
        LocalDate bookingDate,
        List<Integer> slotIndices,
        List<UUID> unitIds,
        List<RuleResult> ruleResults) {
}
