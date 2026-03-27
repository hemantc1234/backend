package com.ticketing.dto;

import com.ticketing.domain.Ticket;
import lombok.Data;

@Data
public class CreateTicketRequest {
    private String subject;
    private String description;
    private String senderEmail;
    private String senderName;
    private Ticket.Priority priority;
    private String assignedTo;
    private String assignedAgentId;
    private String dueDate;   // ISO-8601 string, e.g. "2026-04-01T00:00:00Z"
}
