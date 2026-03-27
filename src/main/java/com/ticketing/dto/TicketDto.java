package com.ticketing.dto;

import com.ticketing.domain.Ticket;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Data @Builder
public class TicketDto {
    private String id;
    private String subject;
    private String description;
    private String senderEmail;
    private String senderName;
    private Ticket.Status status;
    private Ticket.Priority priority;
    private String assignedTo;
    private String assignedAgentId;
    private boolean overdue;
    private Instant dueDate;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant firstRepliedAt;
    private Instant resolvedAt;
    private List<MessageDto> messages;
    private List<AuditDto> auditEvents;

    public static TicketDto from(Ticket t) {
        return TicketDto.builder()
                .id(t.getId()).subject(t.getSubject()).description(t.getDescription())
                .senderEmail(t.getSenderEmail()).senderName(t.getSenderName())
                .status(t.getStatus()).priority(t.getPriority())
                .assignedTo(t.getAssignedTo()).assignedAgentId(t.getAssignedAgentId())
                .overdue(t.isOverdue()).dueDate(t.getDueDate())
                .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .firstRepliedAt(t.getFirstRepliedAt()).resolvedAt(t.getResolvedAt())
                .build();
    }
}
