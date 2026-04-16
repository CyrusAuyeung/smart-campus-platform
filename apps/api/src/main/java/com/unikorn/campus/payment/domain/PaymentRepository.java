package com.unikorn.campus.payment.domain;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository {

    boolean existsTransaction(String transactionNo);

    void createPaymentRecord(UUID orderId, UUID userId, String transactionNo, String status, String callbackPayload);

    void updatePaymentRecordStatus(String transactionNo, String status, String callbackPayload);

    List<ReviewCase> findReviewCases();
}
