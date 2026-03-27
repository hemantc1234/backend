package com.ticketing.controller;

import com.ticketing.domain.Ticket;
import com.ticketing.dto.*;
import com.ticketing.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public Page<TicketDto> list(
            @RequestParam(required = false) String          stage,
            @RequestParam(required = false) Ticket.Status   status,
            @RequestParam(required = false) Ticket.Priority priority,
            @RequestParam(required = false) String          assignedTo,
            @RequestParam(required = false) String          agentId,
            @RequestParam(required = false) String          createdFrom,
            @RequestParam(required = false) String          createdTo,
            @RequestParam(required = false) String          dueFrom,
            @RequestParam(required = false) String          dueTo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ticketService.getTickets(stage, status, priority, assignedTo, agentId,
                createdFrom, createdTo, dueFrom, dueTo, page, size);
    }

    @GetMapping("/stats")
    public StatsDto stats() { return ticketService.getStats(); }

    @GetMapping("/{id}")
    public TicketDto get(@PathVariable String id) { return ticketService.getTicket(id); }

    @PostMapping
    public ResponseEntity<TicketDto> create(@RequestBody CreateTicketRequest req,
                                             @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicket(req, user.getUsername()));
    }

    @PatchMapping("/{id}")
    public TicketDto update(@PathVariable String id, @RequestBody UpdateTicketRequest req,
                            @AuthenticationPrincipal UserDetails user) {
        return ticketService.updateTicket(id, req, user.getUsername());
    }

    @PostMapping("/{id}/notes")
    public MessageDto addNote(@PathVariable String id, @RequestBody AddNoteRequest req,
                              @AuthenticationPrincipal UserDetails user) {
        return ticketService.addNote(id, req, user.getUsername());
    }

    @PostMapping("/{id}/send-email")
    public MessageDto sendEmail(@PathVariable String id, @RequestBody SendEmailRequest req,
                                @AuthenticationPrincipal UserDetails user) {
        return ticketService.sendEmailToCustomer(id, req, user.getUsername());
    }
}
