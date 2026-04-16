package com.unikorn.campus.booking.domain;

import com.unikorn.campus.booking.api.BookingReceipt;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {

    Optional<UserBookingProfile> findUserBookingProfile(UUID userId);

    Optional<UserCreditSnapshot> findUserCreditSnapshot(UUID userId);

    boolean isAcademicSpaceActive(UUID spaceId);

    boolean isSportFacilityActive(UUID facilityId);

    boolean areSportUnitsBelongToFacility(UUID facilityId, List<UUID> unitIds);

    boolean matchesExistingGroup(UUID facilityId, List<UUID> unitIds);

    boolean hasAcademicConflict(UUID spaceId, LocalDateTime effectiveStartAt, LocalDateTime effectiveEndAt);

    boolean hasSportConflict(List<UUID> unitIds, LocalDate bookingDate, List<Integer> slotIndices);

    void createAcademicBooking(AcademicBookingCommand command);

    void createSportBooking(SportBookingCommand command);

    List<BookingReceipt> findBookingsByUserId(UUID userId);

    int transitionBookingStatus(UUID orderId, String expectedStatus, String newStatus);

    int transitionBookingStatusWithVersion(UUID orderId, String expectedStatus, String newStatus, long expectedVersion);

    Optional<BookingReceipt> findBookingById(UUID orderId);

    Optional<BookingOrderSnapshot> findOrderSnapshot(UUID orderId);

    void appendOrderStatusLog(UUID orderId, String previousStatus, String currentStatus, String triggerType,
            String triggerBy);

    void releaseBookingOccupancy(UUID orderId, String businessType);

    void applyNoShowPenalty(UUID userId, int scoreDelta);

    java.util.List<CreditEvent> findCreditEvents(UUID userId);
}
