package com.unikorn.campus.review.domain;

import com.unikorn.campus.payment.domain.PaymentRepository;
import com.unikorn.campus.payment.domain.ReviewCase;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final PaymentRepository paymentRepository;

    public ReviewService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<ReviewCase> listReviewCases() {
        return paymentRepository.findReviewCases();
    }
}