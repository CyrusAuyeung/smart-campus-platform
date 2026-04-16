package com.unikorn.campus.payment.support;

import com.unikorn.campus.payment.domain.ReviewCase;
import com.unikorn.campus.payment.domain.PaymentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPaymentRepository implements PaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsTransaction(String transactionNo) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM payment_record WHERE transaction_no = ?",
                Integer.class,
                transactionNo);
        return count != null && count > 0;
    }

    @Override
    public void createPaymentRecord(UUID orderId, UUID userId, String transactionNo, String status,
            String callbackPayload) {
        jdbcTemplate.update(
                """
                        INSERT INTO payment_record (
                          id,
                          order_id,
                          user_id,
                          transaction_no,
                          status,
                          callback_payload
                        ) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb))
                        """,
                UUID.randomUUID(),
                orderId,
                userId,
                transactionNo,
                status,
                callbackPayload);
    }

    @Override
    public void updatePaymentRecordStatus(String transactionNo, String status, String callbackPayload) {
        jdbcTemplate.update(
                """
                        UPDATE payment_record
                        SET status = ?,
                            callback_payload = CAST(? AS jsonb)
                        WHERE transaction_no = ?
                        """,
                status,
                callbackPayload,
                transactionNo);
    }

    @Override
    public List<ReviewCase> findReviewCases() {
        return jdbcTemplate.query(
                """
                        SELECT transaction_no, order_id, user_id, status, callback_payload, created_at
                        FROM payment_record
                        WHERE status = 'REQUIRES_REVIEW'
                        ORDER BY created_at DESC
                        """,
                (rs, rowNum) -> new ReviewCase(
                        rs.getString("transaction_no"),
                        rs.getObject("order_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("status"),
                        rs.getString("callback_payload"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
    }
}
