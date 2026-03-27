package com.ticketing.dto;
import lombok.*;
@Data @Builder
public class StatsDto {
    // Legacy
    private long totalOpen;
    private long totalInProgress;
    private long totalClosed;
    private long overdueCount;
    // New ticket stage counts
    private long unresolved;   // OPEN + IN_PROGRESS + ON_HOLD
    private long dueToday;     // dueDate = today and not CLOSED
    private long onHold;       // ON_HOLD
    private long unassigned;   // assignedTo IS NULL and not CLOSED
}
