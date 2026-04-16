package com.unikorn.campus.event.domain;

import com.unikorn.campus.event.api.EventItem;
import com.unikorn.campus.event.api.EventHealthSnapshot;
import com.unikorn.campus.event.api.EventRepairAction;
import com.unikorn.campus.event.api.EventReconciliationSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {

    List<EventItem> findPublishedEvents();

    Optional<EventStockSnapshot> findEventStock(UUID eventId);

    boolean hasExistingReservation(UUID eventId, UUID userId);

    boolean hasProcessedRequest(String requestId);

    void createPendingReservation(UUID orderId, String orderNo, UUID eventId, UUID userId, String requestId);

    void markReservationConfirmed(String requestId);

    void markReservationFailed(String requestId, String failureReason);

    void decreasePersistentStock(UUID eventId);

    void increasePersistentStock(UUID eventId);

    List<EventReservationAudit> findReservationAudits();

    EventHealthSnapshot loadHealthSnapshot();

    List<EventReconciliationSnapshot> loadReconciliationSnapshots();

    Optional<Integer> findDatabaseAvailableStock(UUID eventId);

    void createRepairAction(UUID eventId, String actionType, Integer previousCacheStock, int databaseStock,
            String operator);

    List<EventRepairAction> findRepairActions();
}
