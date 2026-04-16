package com.unikorn.campus.credit.domain;

import com.unikorn.campus.booking.domain.BookingRepository;
import com.unikorn.campus.booking.domain.CreditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CreditService {

    private final BookingRepository bookingRepository;

    public CreditService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<CreditEvent> listCreditEvents(UUID userId) {
        return bookingRepository.findCreditEvents(userId);
    }
}