package com.unikorn.campus.review.api;

import com.unikorn.campus.payment.domain.ReviewCase;
import com.unikorn.campus.review.domain.ReviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/payments")
    public List<ReviewCase> listPaymentReviewCases() {
        return reviewService.listReviewCases();
    }
}