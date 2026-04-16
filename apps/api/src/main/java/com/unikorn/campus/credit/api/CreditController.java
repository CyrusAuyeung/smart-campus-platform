package com.unikorn.campus.credit.api;

import com.unikorn.campus.booking.domain.CreditEvent;
import com.unikorn.campus.credit.domain.CreditService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/credits")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("/users/{userId}")
    public List<CreditEvent> listCreditEvents(@PathVariable UUID userId) {
        return creditService.listCreditEvents(userId);
    }
}