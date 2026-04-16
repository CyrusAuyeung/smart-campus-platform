package com.unikorn.campus.event.api;

import com.unikorn.campus.event.domain.EventService;
import com.unikorn.campus.event.domain.EventReservationAudit;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventItem> listEvents() {
        return eventService.listEvents();
    }

    @PostMapping("/reserve")
    public EventReserveReceipt reserve(@Valid @RequestBody EventReserveRequest request) {
        return eventService.reserve(request);
    }

    @GetMapping("/health")
    public EventHealthSnapshot health() {
        return eventService.loadHealthSnapshot();
    }

    @GetMapping("/audits")
    public List<EventReservationAudit> audits() {
        return eventService.listReservationAudits();
    }

    @GetMapping("/reconciliation")
    public List<EventReconciliationSnapshot> reconciliation() {
        return eventService.loadReconciliationSnapshots();
    }

    @PostMapping("/{eventId}/reconcile")
    public EventReconciliationSnapshot reconcile(@PathVariable UUID eventId) {
        return eventService.reconcileEventStock(eventId);
    }

    @GetMapping("/repairs")
    public List<EventRepairAction> repairs() {
        return eventService.listRepairActions();
    }
}
