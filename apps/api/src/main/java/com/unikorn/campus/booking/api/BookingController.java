package com.unikorn.campus.booking.api;

import com.unikorn.campus.booking.domain.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/academic")
    public BookingReceipt createAcademicBooking(@Valid @RequestBody AcademicBookingRequest request) {
        return bookingService.createAcademicBooking(request);
    }

    @PostMapping("/sport")
    public BookingReceipt createSportBooking(@Valid @RequestBody SportBookingRequest request) {
        return bookingService.createSportBooking(request);
    }

    @GetMapping("/users/{userId}")
    public List<BookingReceipt> listUserBookings(@PathVariable UUID userId) {
        return bookingService.listBookings(userId);
    }

    @PatchMapping("/{orderId}/confirm/users/{userId}")
    public BookingReceipt confirmBooking(@PathVariable UUID orderId, @PathVariable UUID userId) {
        return bookingService.confirmPayment(orderId, userId);
    }

    @PatchMapping("/{orderId}/cancel/users/{userId}")
    public BookingReceipt cancelBooking(@PathVariable UUID orderId, @PathVariable UUID userId) {
        return bookingService.cancelBooking(orderId, userId);
    }

    @PatchMapping("/{orderId}/timeout")
    public BookingReceipt timeoutBooking(@PathVariable UUID orderId) {
        return bookingService.timeoutBooking(orderId);
    }

    @PatchMapping("/{orderId}/no-show/users/{userId}")
    public BookingReceipt markNoShow(@PathVariable UUID orderId, @PathVariable UUID userId) {
        return bookingService.markNoShow(orderId, userId);
    }
}
