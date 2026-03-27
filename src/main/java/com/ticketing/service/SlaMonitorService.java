package com.ticketing.service;

import com.ticketing.domain.*;
import com.ticketing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlaMonitorService {

    private final TicketRepository           ticketRepo;
    private final TicketAuditEventRepository auditRepo;
    private final SlaPolicyRepository        slaPolicyRepo;
    private final NotificationService        notifier;

    @Scheduled(fixedDelayString = "${app.sla.scan-interval-ms:300000}")
    @Transactional
    public void scan() {
        var candidates = ticketRepo.findOpenNonOverdue();
        if (candidates.isEmpty()) return;

        int breached = 0;
        for (Ticket ticket : candidates) {
            var policy = slaPolicyRepo.findByPriority(ticket.getPriority());
            if (policy.isEmpty()) continue;

            long elapsed = Duration.between(ticket.getCreatedAt(), Instant.now()).toSeconds();
            long limit   = ticket.getFirstRepliedAt() == null
                    ? policy.get().getFirstResponseSec()
                    : policy.get().getResolutionSec();

            if (elapsed > limit) {
                ticket.setOverdue(true);
                ticketRepo.save(ticket);
                auditRepo.save(TicketAuditEvent.builder()
                        .ticket(ticket).actor("SYSTEM")
                        .eventType(TicketAuditEvent.EventType.SLA_BREACHED)
                        .newValue("overdue after " + elapsed + "s (limit: " + limit + "s)")
                        .build());
                notifier.sendSlaAlert(ticket);
                breached++;
            }
        }
        if (breached > 0) log.warn("SLA scan: {} ticket(s) marked overdue", breached);
    }
}
