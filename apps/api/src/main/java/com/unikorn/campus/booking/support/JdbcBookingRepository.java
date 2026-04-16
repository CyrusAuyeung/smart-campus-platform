package com.unikorn.campus.booking.support;

import com.unikorn.campus.booking.api.BookingReceipt;
import com.unikorn.campus.booking.domain.AcademicBookingCommand;
import com.unikorn.campus.booking.domain.BookingOrderSnapshot;
import com.unikorn.campus.booking.domain.BookingRepository;
import com.unikorn.campus.booking.domain.CreditEvent;
import com.unikorn.campus.booking.domain.SportBookingCommand;
import com.unikorn.campus.booking.domain.UserCreditSnapshot;
import com.unikorn.campus.booking.domain.UserBookingProfile;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcBookingRepository implements BookingRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcBookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserBookingProfile> findUserBookingProfile(UUID userId) {
        List<UserBookingProfile> profiles = jdbcTemplate.query(
                """
                        SELECT id, role_code, credit_score, recent_no_show_count
                        FROM app_user
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new UserBookingProfile(
                        rs.getObject("id", UUID.class),
                        rs.getString("role_code"),
                        rs.getInt("credit_score"),
                        rs.getInt("recent_no_show_count")),
                userId);
        return profiles.stream().findFirst();
    }

    @Override
    public Optional<UserCreditSnapshot> findUserCreditSnapshot(UUID userId) {
        List<UserCreditSnapshot> snapshots = jdbcTemplate.query(
                """
                        SELECT id, credit_score, recent_no_show_count
                        FROM app_user
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new UserCreditSnapshot(
                        rs.getObject("id", UUID.class),
                        rs.getInt("credit_score"),
                        rs.getInt("recent_no_show_count")),
                userId);
        return snapshots.stream().findFirst();
    }

    @Override
    public boolean isAcademicSpaceActive(UUID spaceId) {
        return existsActiveResource(spaceId, "ACADEMIC_SPACE");
    }

    @Override
    public boolean isSportFacilityActive(UUID facilityId) {
        return existsActiveResource(facilityId, "SPORT_FACILITY");
    }

    @Override
    public boolean areSportUnitsBelongToFacility(UUID facilityId, List<UUID> unitIds) {
        Integer count = jdbcTemplate.query(
                """
                        SELECT COUNT(1)
                        FROM sport_unit
                        WHERE facility_id = ?
                          AND id = ANY (?::uuid[])
                          AND status = 'ACTIVE'
                        """,
                ps -> {
                    ps.setObject(1, facilityId);
                    ps.setArray(2, uuidArray(ps, unitIds));
                },
                rs -> rs.next() ? rs.getInt(1) : 0);
        return count != null && count == unitIds.size();
    }

    @Override
    public boolean matchesExistingGroup(UUID facilityId, List<UUID> unitIds) {
        Integer count = jdbcTemplate.query(
                """
                        SELECT COUNT(*)
                        FROM sport_unit_group g
                        JOIN sport_unit_group_member m ON g.id = m.group_id
                        WHERE g.facility_id = ?
                        GROUP BY g.id
                        HAVING COUNT(*) = ?
                           AND COUNT(*) FILTER (WHERE m.unit_id = ANY (?::uuid[])) = ?
                        """,
                ps -> {
                    ps.setObject(1, facilityId);
                    ps.setInt(2, unitIds.size());
                    ps.setArray(3, uuidArray(ps, unitIds));
                    ps.setInt(4, unitIds.size());
                },
                                rs -> rs.next() ? rs.getInt(1) : 0);
        return count != null && count > 0;
    }

    @Override
    public boolean hasAcademicConflict(UUID spaceId, LocalDateTime effectiveStartAt, LocalDateTime effectiveEndAt) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM academic_booking_occupancy
                        WHERE space_id = ?
                          AND effective_range && tsrange(?, ?, '[)')
                        """,
                Integer.class,
                spaceId,
                effectiveStartAt,
                effectiveEndAt);
        return count != null && count > 0;
    }

    @Override
    public boolean hasSportConflict(List<UUID> unitIds, LocalDate bookingDate, List<Integer> slotIndices) {
        Integer count = jdbcTemplate.query(
                """
                        SELECT COUNT(1)
                        FROM sport_booking_occupancy
                        WHERE booking_date = ?
                          AND unit_id = ANY (?::uuid[])
                          AND slot_index = ANY (?::int[])
                        """,
                ps -> {
                    ps.setObject(1, bookingDate);
                    ps.setArray(2, uuidArray(ps, unitIds));
                    ps.setArray(3, intArray(ps, slotIndices));
                },
                                rs -> rs.next() ? rs.getInt(1) : 0);
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void createAcademicBooking(AcademicBookingCommand command) {
        insertOrder(
                command.orderId(),
                command.orderNo(),
                command.userId(),
                "ACADEMIC",
                "ACADEMIC_SPACE",
                command.paymentDeadline());

        jdbcTemplate.update(
                """
                        INSERT INTO academic_booking_occupancy (
                          id,
                          order_id,
                          space_id,
                          display_start_at,
                          display_end_at,
                          effective_range
                        ) VALUES (?, ?, ?, ?, ?, tsrange(?, ?, '[)'))
                        """,
                UUID.randomUUID(),
                command.orderId(),
                command.spaceId(),
                command.displayStartAt(),
                command.displayEndAt(),
                command.effectiveStartAt(),
                command.effectiveEndAt());

        insertOrderStatusLog(command.orderId(), null, "PENDING_PAYMENT", "SYSTEM", "booking-service");
    }

    @Override
    @Transactional
    public void createSportBooking(SportBookingCommand command) {
        insertOrder(
                command.orderId(),
                command.orderNo(),
                command.userId(),
                "SPORT",
                "SPORT_FACILITY",
                command.paymentDeadline());

        for (UUID unitId : command.unitIds()) {
            for (Integer slotIndex : command.slotIndices()) {
                jdbcTemplate.update(
                        """
                                INSERT INTO sport_booking_occupancy (
                                  id,
                                  order_id,
                                  facility_id,
                                  unit_id,
                                  booking_date,
                                  slot_index
                                ) VALUES (?, ?, ?, ?, ?, ?)
                                """,
                        UUID.randomUUID(),
                        command.orderId(),
                        command.facilityId(),
                        unitId,
                        command.request().bookingDate(),
                        slotIndex);
            }
        }

        insertOrderStatusLog(command.orderId(), null, "PENDING_PAYMENT", "SYSTEM", "booking-service");
    }

    @Override
    public List<BookingReceipt> findBookingsByUserId(UUID userId) {
        return jdbcTemplate.query(
                """
                        SELECT
                          bo.id AS order_id,
                          bo.order_no,
                          bo.status,
                          bo.business_type,
                          bo.resource_type,
                          abo.space_id,
                          sbo.facility_id,
                          abo.display_start_at,
                          abo.display_end_at,
                          lower(abo.effective_range) AS effective_start_at,
                          upper(abo.effective_range) AS effective_end_at,
                          sbo.booking_date,
                          ARRAY_REMOVE(ARRAY_AGG(DISTINCT sbo.slot_index), NULL) AS slot_indices,
                          ARRAY_REMOVE(ARRAY_AGG(DISTINCT sbo.unit_id), NULL) AS unit_ids
                        FROM booking_order bo
                        LEFT JOIN academic_booking_occupancy abo ON abo.order_id = bo.id
                        LEFT JOIN sport_booking_occupancy sbo ON sbo.order_id = bo.id
                        WHERE bo.user_id = ?
                        GROUP BY bo.id, bo.order_no, bo.status, bo.business_type, bo.resource_type,
                                 abo.space_id, sbo.facility_id, abo.display_start_at, abo.display_end_at,
                                 abo.effective_range, sbo.booking_date
                        ORDER BY bo.created_at DESC
                        """,
                (rs, rowNum) -> new BookingReceipt(
                        rs.getObject("order_id", UUID.class),
                        rs.getString("order_no"),
                        rs.getString("status"),
                        rs.getString("business_type"),
                        rs.getString("resource_type"),
                        rs.getObject("space_id") != null
                                ? rs.getObject("space_id", UUID.class)
                                : rs.getObject("facility_id", UUID.class),
                        rs.getString("business_type").equals("ACADEMIC")
                                ? "学术空间预约，已自动注入缓冲期"
                                : "体育设施预约，已锁定对应场地单元和槽位",
                        rs.getTimestamp("display_start_at") != null
                                ? rs.getTimestamp("display_start_at").toLocalDateTime()
                                : null,
                        rs.getTimestamp("display_end_at") != null
                                ? rs.getTimestamp("display_end_at").toLocalDateTime()
                                : null,
                        rs.getTimestamp("effective_start_at") != null
                                ? rs.getTimestamp("effective_start_at").toLocalDateTime()
                                : null,
                        rs.getTimestamp("effective_end_at") != null
                                ? rs.getTimestamp("effective_end_at").toLocalDateTime()
                                : null,
                        rs.getDate("booking_date") != null ? rs.getDate("booking_date").toLocalDate() : null,
                        readIntegerList(rs.getArray("slot_indices")),
                        readUuidList(rs.getArray("unit_ids")),
                        List.of()),
                userId);
    }

    @Override
    public int transitionBookingStatus(UUID orderId, String expectedStatus, String newStatus) {
        return jdbcTemplate.update(
                """
                        UPDATE booking_order
                        SET status = ?,
                            version = version + 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                          AND status = ?
                        """,
                newStatus,
                orderId,
                expectedStatus);
    }

    @Override
    public int transitionBookingStatusWithVersion(UUID orderId, String expectedStatus, String newStatus,
            long expectedVersion) {
        return jdbcTemplate.update(
                """
                        UPDATE booking_order
                        SET status = ?,
                                        version = version + 1,
                                        updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                                AND status = ?
                                AND version = ?
                        """,
                newStatus,
                orderId,
                expectedStatus,
                expectedVersion);
    }

    @Override
    public Optional<BookingReceipt> findBookingById(UUID orderId) {
        List<BookingReceipt> receipts = jdbcTemplate.query(
                """
                        SELECT
                          bo.id AS order_id,
                          bo.order_no,
                          bo.status,
                          bo.business_type,
                          bo.resource_type,
                          abo.space_id,
                          sbo.facility_id,
                          abo.display_start_at,
                          abo.display_end_at,
                          lower(abo.effective_range) AS effective_start_at,
                          upper(abo.effective_range) AS effective_end_at,
                          sbo.booking_date,
                          ARRAY_REMOVE(ARRAY_AGG(DISTINCT sbo.slot_index), NULL) AS slot_indices,
                          ARRAY_REMOVE(ARRAY_AGG(DISTINCT sbo.unit_id), NULL) AS unit_ids
                        FROM booking_order bo
                        LEFT JOIN academic_booking_occupancy abo ON abo.order_id = bo.id
                        LEFT JOIN sport_booking_occupancy sbo ON sbo.order_id = bo.id
                        WHERE bo.id = ?
                        GROUP BY bo.id, bo.order_no, bo.status, bo.business_type, bo.resource_type,
                                 abo.space_id, sbo.facility_id, abo.display_start_at, abo.display_end_at,
                                 abo.effective_range, sbo.booking_date
                        """,
                (rs, rowNum) -> new BookingReceipt(
                        rs.getObject("order_id", UUID.class),
                        rs.getString("order_no"),
                        rs.getString("status"),
                        rs.getString("business_type"),
                        rs.getString("resource_type"),
                        rs.getObject("space_id") != null
                                ? rs.getObject("space_id", UUID.class)
                                : rs.getObject("facility_id", UUID.class),
                        rs.getString("business_type").equals("ACADEMIC")
                                ? "学术空间预约，已自动注入缓冲期"
                                : "体育设施预约，已锁定对应场地单元和槽位",
                        rs.getTimestamp("display_start_at") != null
                                ? rs.getTimestamp("display_start_at").toLocalDateTime()
                                : null,
                        rs.getTimestamp("display_end_at") != null
                                ? rs.getTimestamp("display_end_at").toLocalDateTime()
                                : null,
                        rs.getTimestamp("effective_start_at") != null
                                ? rs.getTimestamp("effective_start_at").toLocalDateTime()
                                : null,
                        rs.getTimestamp("effective_end_at") != null
                                ? rs.getTimestamp("effective_end_at").toLocalDateTime()
                                : null,
                        rs.getDate("booking_date") != null ? rs.getDate("booking_date").toLocalDate() : null,
                        readIntegerList(rs.getArray("slot_indices")),
                        readUuidList(rs.getArray("unit_ids")),
                        List.of()),
                orderId);
        return receipts.stream().findFirst();
    }

    @Override
    public Optional<BookingOrderSnapshot> findOrderSnapshot(UUID orderId) {
        List<BookingOrderSnapshot> snapshots = jdbcTemplate.query(
                """
                        SELECT id, user_id, order_no, status, business_type, resource_type, version, payment_deadline
                        FROM booking_order
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new BookingOrderSnapshot(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("order_no"),
                        rs.getString("status"),
                        rs.getString("business_type"),
                        rs.getString("resource_type"),
                        rs.getLong("version"),
                        rs.getTimestamp("payment_deadline") != null
                                ? rs.getTimestamp("payment_deadline").toLocalDateTime()
                                : null),
                orderId);
        return snapshots.stream().findFirst();
    }

    @Override
    public void appendOrderStatusLog(
            UUID orderId,
            String previousStatus,
            String currentStatus,
            String triggerType,
            String triggerBy) {
        insertOrderStatusLog(orderId, previousStatus, currentStatus, triggerType, triggerBy);
    }

    @Override
    public void releaseBookingOccupancy(UUID orderId, String businessType) {
        if ("ACADEMIC".equalsIgnoreCase(businessType)) {
            jdbcTemplate.update("DELETE FROM academic_booking_occupancy WHERE order_id = ?", orderId);
            return;
        }

        if ("SPORT".equalsIgnoreCase(businessType)) {
            jdbcTemplate.update("DELETE FROM sport_booking_occupancy WHERE order_id = ?", orderId);
        }
    }

    @Override
    public void applyNoShowPenalty(UUID userId, int scoreDelta) {
        jdbcTemplate.update(
                """
                        UPDATE app_user
                        SET credit_score = GREATEST(0, credit_score + ?),
                                recent_no_show_count = recent_no_show_count + 1,
                                updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                scoreDelta,
                userId);

        jdbcTemplate.update(
                """
                        INSERT INTO credit_event (
                          id,
                          user_id,
                          event_type,
                          score_delta,
                          reason
                        ) VALUES (?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                userId,
                "NO_SHOW",
                scoreDelta,
                "爽约扣分");
    }

    @Override
    public List<CreditEvent> findCreditEvents(UUID userId) {
        return jdbcTemplate.query(
                """
                        SELECT id, user_id, event_type, score_delta, reason, created_at
                        FROM credit_event
                        WHERE user_id = ?
                        ORDER BY created_at DESC
                        """,
                (rs, rowNum) -> new CreditEvent(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("event_type"),
                        rs.getInt("score_delta"),
                        rs.getString("reason"),
                        rs.getTimestamp("created_at").toLocalDateTime()),
                userId);
    }

    private void insertOrder(
            UUID orderId,
            String orderNo,
            UUID userId,
            String businessType,
            String resourceType,
            LocalDateTime paymentDeadline) {
        jdbcTemplate.update(
                """
                        INSERT INTO booking_order (
                          id,
                          order_no,
                          user_id,
                          business_type,
                          resource_type,
                          status,
                          payment_deadline,
                          version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                orderId,
                orderNo,
                userId,
                businessType,
                resourceType,
                "PENDING_PAYMENT",
                paymentDeadline);
    }

    private void insertOrderStatusLog(
            UUID orderId,
            String previousStatus,
            String currentStatus,
            String triggerType,
            String triggerBy) {
        jdbcTemplate.update(
                """
                        INSERT INTO order_status_log (
                          id,
                          order_id,
                          previous_status,
                          current_status,
                          trigger_type,
                          trigger_by
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                orderId,
                previousStatus,
                currentStatus,
                triggerType,
                triggerBy);
    }

    private Array uuidArray(PreparedStatement statement, List<UUID> values) throws SQLException {
        return statement.getConnection().createArrayOf("uuid", values.toArray());
    }

    private Array intArray(PreparedStatement statement, List<Integer> values) throws SQLException {
        return statement.getConnection().createArrayOf("integer", values.toArray());
    }

    private boolean existsActiveResource(UUID resourceId, String resourceType) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM resource_space
                        WHERE id = ?
                          AND resource_type = ?
                          AND status = 'ACTIVE'
                        """,
                Integer.class,
                resourceId,
                resourceType);
        return count != null && count > 0;
    }

    private List<Integer> readIntegerList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }

        Object[] values = (Object[]) sqlArray.getArray();
        if (values == null) {
            return List.of();
        }

        return java.util.Arrays.stream(values)
                .filter(java.util.Objects::nonNull)
                .map(value -> ((Number) value).intValue())
                .sorted()
                .toList();
    }

    private List<UUID> readUuidList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }

        Object[] values = (Object[]) sqlArray.getArray();
        if (values == null) {
            return List.of();
        }

        return java.util.Arrays.stream(values)
                .filter(java.util.Objects::nonNull)
                .map(value -> value instanceof UUID uuid ? uuid : UUID.fromString(String.valueOf(value)))
                .sorted()
                .toList();
    }
}
