package com.unikorn.campus.event.support;

import com.unikorn.campus.event.api.EventItem;
import com.unikorn.campus.event.api.EventHealthSnapshot;
import com.unikorn.campus.event.api.EventReconciliationSnapshot;
import com.unikorn.campus.event.api.EventRepairAction;
import com.unikorn.campus.event.domain.EventReservationAudit;
import com.unikorn.campus.event.domain.EventRepository;
import com.unikorn.campus.event.domain.EventStockSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcEventRepository implements EventRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<EventItem> findPublishedEvents() {
        return jdbcTemplate.query(
                """
                        SELECT id, event_code, title, status, starts_at, ends_at, total_stock, available_stock, limit_per_user
                        FROM campus_event
                        WHERE status = 'PUBLISHED'
                        ORDER BY starts_at
                        """,
                (rs, rowNum) -> new EventItem(
                        rs.getString("id"),
                        rs.getString("event_code"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getTimestamp("starts_at").toLocalDateTime(),
                        rs.getTimestamp("ends_at").toLocalDateTime(),
                        rs.getInt("total_stock"),
                        rs.getInt("available_stock"),
                        rs.getInt("limit_per_user")));
    }

    @Override
    public Optional<EventStockSnapshot> findEventStock(UUID eventId) {
        List<EventStockSnapshot> rows = jdbcTemplate.query(
                """
                        SELECT id, title, available_stock, limit_per_user, status
                        FROM campus_event
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new EventStockSnapshot(
                        rs.getObject("id", UUID.class),
                        rs.getString("title"),
                        rs.getInt("available_stock"),
                        rs.getInt("limit_per_user"),
                        rs.getString("status")),
                eventId);
        return rows.stream().findFirst();
    }

    @Override
    public boolean hasExistingReservation(UUID eventId, UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM event_order
                        WHERE event_id = ?
                          AND user_id = ?
                          AND status IN ('PENDING', 'CONFIRMED')
                        """,
                Integer.class,
                eventId,
                userId);
        return count != null && count > 0;
    }

    @Override
    public boolean hasProcessedRequest(String requestId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM event_order WHERE request_id = ?",
                Integer.class,
                requestId);
        return count != null && count > 0;
    }

    @Override
    public void createPendingReservation(UUID orderId, String orderNo, UUID eventId, UUID userId, String requestId) {
        jdbcTemplate.update(
                """
                        INSERT INTO event_order (id, event_id, user_id, order_no, status, request_id)
                        VALUES (?, ?, ?, ?, 'PENDING', ?)
                        """,
                orderId,
                eventId,
                userId,
                orderNo,
                requestId);
    }

    @Override
    public void markReservationConfirmed(String requestId) {
        jdbcTemplate.update(
                """
                        UPDATE event_order
                        SET status = 'CONFIRMED'
                        WHERE request_id = ?
                        """,
                requestId);
    }

    @Override
    public void markReservationFailed(String requestId, String failureReason) {
        jdbcTemplate.update(
                """
                        UPDATE event_order
                        SET status = 'FAILED',
                            failure_reason = ?
                        WHERE request_id = ?
                        """,
                failureReason,
                requestId);
    }

    @Override
    public void decreasePersistentStock(UUID eventId) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE campus_event
                        SET available_stock = available_stock - 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                          AND available_stock > 0
                        """,
                eventId);
        if (updated == 0) {
            throw new IllegalStateException("数据库库存扣减失败");
        }
    }

    @Override
    public void increasePersistentStock(UUID eventId) {
        jdbcTemplate.update(
                """
                        UPDATE campus_event
                        SET available_stock = available_stock + 1,
                                updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                eventId);
    }

    @Override
    public List<EventReservationAudit> findReservationAudits() {
        return jdbcTemplate.query(
                """
                        SELECT request_id, event_id, user_id, status, failure_reason, created_at
                        FROM event_order
                        ORDER BY created_at DESC
                        """,
                (rs, rowNum) -> new EventReservationAudit(
                        rs.getString("request_id"),
                        rs.getObject("event_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("status"),
                        rs.getString("failure_reason"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
    }

    @Override
    public EventHealthSnapshot loadHealthSnapshot() {
        return jdbcTemplate.queryForObject(
                """
                        SELECT
                          COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_orders,
                          COUNT(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_orders,
                          COUNT(*) FILTER (WHERE status = 'FAILED') AS failed_orders
                        FROM event_order
                        """,
                (rs, rowNum) -> new EventHealthSnapshot(
                        rs.getLong("pending_orders"),
                        rs.getLong("confirmed_orders"),
                        rs.getLong("failed_orders")));
    }

    @Override
    public List<EventReconciliationSnapshot> loadReconciliationSnapshots() {
        return jdbcTemplate.query(
                """
                        SELECT id::text AS event_id, available_stock
                        FROM campus_event
                        ORDER BY event_code
                        """,
                (rs, rowNum) -> new EventReconciliationSnapshot(
                        rs.getString("event_id"),
                        rs.getInt("available_stock"),
                        -1,
                        false));
    }

    @Override
    public Optional<Integer> findDatabaseAvailableStock(UUID eventId) {
        List<Integer> stocks = jdbcTemplate.query(
                "SELECT available_stock FROM campus_event WHERE id = ?",
                (rs, rowNum) -> rs.getInt("available_stock"),
                eventId);
        return stocks.stream().findFirst();
    }

    @Override
    public void createRepairAction(UUID eventId, String actionType, Integer previousCacheStock, int databaseStock,
            String operator) {
        jdbcTemplate.update(
                """
                        INSERT INTO event_repair_action (
                          id,
                          event_id,
                          action_type,
                          previous_cache_stock,
                          database_stock,
                          operator
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                eventId,
                actionType,
                previousCacheStock,
                databaseStock,
                operator);
    }

    @Override
    public List<EventRepairAction> findRepairActions() {
        return jdbcTemplate.query(
                """
                        SELECT event_id::text AS event_id, action_type, previous_cache_stock, database_stock, operator, created_at
                        FROM event_repair_action
                        ORDER BY created_at DESC
                        """,
                (rs, rowNum) -> new EventRepairAction(
                        rs.getString("event_id"),
                        rs.getString("action_type"),
                        rs.getObject("previous_cache_stock", Integer.class),
                        rs.getInt("database_stock"),
                        rs.getString("operator"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
    }
}
