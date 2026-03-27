package com.ticketing.service;

import com.ticketing.domain.*;
import com.ticketing.dto.*;
import com.ticketing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository           ticketRepo;
    private final TicketMessageRepository    messageRepo;
    private final TicketAuditEventRepository auditRepo;
    private final ThreadingService           threading;
    private final RoutingEngineService       routing;
    private final NotificationService        notifier;

    // ── Inbound email pipeline ────────────────────────────────────────────────

    @Async
    @Transactional
    public void processAsync(ParsedEmail email) {
        try { process(email); }
        catch (Exception e) { log.error("Failed to process email {}: {}", email.getMessageId(), e.getMessage(), e); }
    }

    @Transactional
    public void process(ParsedEmail email) {
        if (threading.isDuplicate(email.getMessageId())) {
            log.info("Duplicate messageId skipped: {}", email.getMessageId());
            return;
        }
        Optional<Ticket> existing = threading.findParentTicket(email.getInReplyTo());
        if (existing.isPresent()) appendToThread(existing.get(), email);
        else createFromEmail(email);
    }

    private void createFromEmail(ParsedEmail email) {
        Ticket ticket = Ticket.builder()
                .subject(email.getSubject() != null ? email.getSubject() : "(no subject)")
                .description(email.getBody())
                .senderEmail(email.getSenderEmail())
                .senderName(email.getSenderName())
                .assignedTo(routing.assign(email))
                .assignedAgentId(routing.resolveAgentId(email).orElse(null))
                .mailboxId(email.getMailboxId())
                .build();
        ticket = ticketRepo.save(ticket);
        messageRepo.save(TicketMessage.builder()
                .ticket(ticket).messageId(email.getMessageId())
                .inReplyTo(email.getInReplyTo()).body(email.getBody())
                .senderEmail(email.getSenderEmail()).build());
        audit(ticket, "SYSTEM", TicketAuditEvent.EventType.TICKET_CREATED, null, Ticket.Status.OPEN.name(), "Created from inbound email");
        notifier.sendAck(ticket);
    }

    private void appendToThread(Ticket ticket, ParsedEmail email) {
        messageRepo.save(TicketMessage.builder()
                .ticket(ticket).messageId(email.getMessageId())
                .inReplyTo(email.getInReplyTo()).body(email.getBody())
                .senderEmail(email.getSenderEmail()).build());
        if (ticket.getStatus() == Ticket.Status.CLOSED) {
            audit(ticket, "SYSTEM", TicketAuditEvent.EventType.STATUS_CHANGED, Ticket.Status.CLOSED.name(), Ticket.Status.OPEN.name(), "Re-opened on customer reply");
            ticket.setStatus(Ticket.Status.OPEN);
        }
        ticketRepo.save(ticket);
    }

    // ── Manual ticket creation from UI ────────────────────────────────────────

    @Transactional
    public TicketDto createTicket(CreateTicketRequest req, String actor) {
        Ticket ticket = Ticket.builder()
                .subject(req.getSubject())
                .description(req.getDescription())
                .senderEmail(req.getSenderEmail() != null ? req.getSenderEmail() : actor + "@support")
                .senderName(req.getSenderName())
                .priority(req.getPriority() != null ? req.getPriority() : Ticket.Priority.MEDIUM)
                .assignedTo(req.getAssignedTo())
                .assignedAgentId(req.getAssignedAgentId())
                .dueDate(parseInstant(req.getDueDate()))
                .build();
        ticket = ticketRepo.save(ticket);
        audit(ticket, actor, TicketAuditEvent.EventType.TICKET_CREATED, null, Ticket.Status.OPEN.name(), "Created manually by " + actor);
        return TicketDto.from(ticket);
    }

    // ── Update ticket ─────────────────────────────────────────────────────────

    @Transactional
    public TicketDto updateTicket(String id, UpdateTicketRequest req, String actor) {
        Ticket ticket = ticketRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ticket not found: " + id));

        if (req.getStatus() != null && !req.getStatus().equals(ticket.getStatus())) {
            audit(ticket, actor, TicketAuditEvent.EventType.STATUS_CHANGED, ticket.getStatus().name(), req.getStatus().name(), null);
            ticket.setStatus(req.getStatus());
            if (req.getStatus() == Ticket.Status.CLOSED) ticket.setResolvedAt(java.time.Instant.now());
        }
        if (req.getPriority() != null && !req.getPriority().equals(ticket.getPriority())) {
            audit(ticket, actor, TicketAuditEvent.EventType.PRIORITY_CHANGED, ticket.getPriority().name(), req.getPriority().name(), null);
            ticket.setPriority(req.getPriority());
        }
        if (req.getAssignedTo() != null) {
            audit(ticket, actor, TicketAuditEvent.EventType.ASSIGNED, ticket.getAssignedTo(), req.getAssignedTo(), null);
            ticket.setAssignedTo(req.getAssignedTo());
        }
        if (req.getAssignedAgentId() != null) ticket.setAssignedAgentId(req.getAssignedAgentId());
        if (req.getDueDate() != null) ticket.setDueDate(parseInstant(req.getDueDate()));
        return TicketDto.from(ticketRepo.save(ticket));
    }

    @Transactional
    public MessageDto addNote(String ticketId, AddNoteRequest req, String actor) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Ticket not found: " + ticketId));
        TicketMessage msg = messageRepo.save(TicketMessage.builder()
                .ticket(ticket).messageId("NOTE-" + UUID.randomUUID())
                .body(req.getBody()).senderEmail(actor)
                .direction(TicketMessage.Direction.OUTBOUND)
                .internal(req.isInternal()).build());
        audit(ticket, actor, req.isInternal() ? TicketAuditEvent.EventType.NOTE_ADDED : TicketAuditEvent.EventType.REPLY_ADDED,
              null, null, req.getBody().substring(0, Math.min(80, req.getBody().length())));
        if (!req.isInternal() && ticket.getFirstRepliedAt() == null) {
            ticket.setFirstRepliedAt(java.time.Instant.now());
            ticketRepo.save(ticket);
        }
        return MessageDto.from(msg);
    }

    @Transactional
    public MessageDto sendEmailToCustomer(String ticketId, SendEmailRequest req, String actor) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Ticket not found: " + ticketId));
        TicketMessage msg = messageRepo.save(TicketMessage.builder()
                .ticket(ticket).messageId("EMAIL-" + UUID.randomUUID())
                .body(req.getBody()).senderEmail(actor)
                .direction(TicketMessage.Direction.OUTBOUND).internal(false).build());
        notifier.sendReplyToCustomer(ticket, req.getSubject(), req.getBody(), actor);
        audit(ticket, actor, TicketAuditEvent.EventType.REPLY_ADDED, null, null, "Email sent to " + ticket.getSenderEmail());
        if (ticket.getFirstRepliedAt() == null) {
            ticket.setFirstRepliedAt(java.time.Instant.now());
            ticketRepo.save(ticket);
        }
        return MessageDto.from(msg);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TicketDto> getTickets(String stage, Ticket.Status status, Ticket.Priority priority,
                                       String assignedTo, String agentId,
                                       String createdFrom, String createdTo,
                                       String dueFrom, String dueTo,
                                       int page, int size) {
        Instant dayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd   = dayStart.plus(1, ChronoUnit.DAYS);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ticketRepo.search(stage, status, priority, assignedTo, agentId,
                parseInstant(createdFrom), parseInstant(createdTo),
                parseInstant(dueFrom), parseInstant(dueTo),
                dayStart, dayEnd, pageable)
                .map(TicketDto::from);
    }

    @Transactional(readOnly = true)
    public TicketDto getTicket(String id) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ticket not found: " + id));
        TicketDto dto = TicketDto.from(t);
        dto.setMessages(messageRepo.findByTicketIdOrderByCreatedAtAsc(id).stream().map(MessageDto::from).toList());
        dto.setAuditEvents(auditRepo.findByTicketIdOrderByCreatedAtDesc(id).stream().map(AuditDto::from).toList());
        return dto;
    }

    @Transactional(readOnly = true)
    public StatsDto getStats() {
        Instant dayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd   = dayStart.plus(1, ChronoUnit.DAYS);
        return StatsDto.builder()
                .totalOpen(ticketRepo.countByStatus(Ticket.Status.OPEN))
                .totalInProgress(ticketRepo.countByStatus(Ticket.Status.IN_PROGRESS))
                .totalClosed(ticketRepo.countByStatus(Ticket.Status.CLOSED))
                .overdueCount(ticketRepo.countByOverdueTrue())
                .unresolved(ticketRepo.countUnresolved())
                .dueToday(ticketRepo.countDueToday(dayStart, dayEnd))
                .onHold(ticketRepo.countByStatus(Ticket.Status.ON_HOLD))
                .unassigned(ticketRepo.countUnassigned())
                .build();
    }

    private void audit(Ticket ticket, String actor, TicketAuditEvent.EventType type, String oldVal, String newVal, String note) {
        auditRepo.save(TicketAuditEvent.builder()
                .ticket(ticket).actor(actor).eventType(type)
                .oldValue(oldVal).newValue(newVal).note(note).build());
    }

    private java.time.Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return null;
        try { return java.time.Instant.parse(s); } catch (Exception e) { return null; }
    }
}
