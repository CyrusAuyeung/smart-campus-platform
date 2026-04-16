package com.unikorn.campus.booking.domain;

import com.unikorn.campus.booking.api.SportBookingRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SportBookingCommand(
        UUID orderId,
        String orderNo,
        UUID userId,
        UUID facilityId,
        List<UUID> unitIds,
        List<Integer> slotIndices,
        LocalDateTime paymentDeadline,
        SportBookingRequest request) {
    public static SportBookingCommand of(
            UUID orderId,
            String orderNo,
            SportBookingRequest request,
            LocalDateTime paymentDeadline) {
        return new SportBookingCommand(
                orderId,
                orderNo,
                request.userId(),
                request.facilityId(),
                List.copyOf(request.unitIds()),
                List.copyOf(request.slotIndices()),
                paymentDeadline,
                request);
    }
}
