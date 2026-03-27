package com.ticketing.dto;
import com.ticketing.domain.Ticket;
import lombok.Data;
@Data
public class UpdateTicketRequest {
    private Ticket.Status   status;
    private Ticket.Priority priority;
    private String assignedTo;
    private String assignedAgentId;
    private String dueDate;   // ISO-8601 string or null to clear
}
