package com.ticketing.dto;

import com.ticketing.domain.TicketAuditEvent;
import lombok.*;
import java.time.Instant;

@Data @Builder
public class AuditDto {
    private String id;
    private String actor;
    private TicketAuditEvent.EventType eventType;
    private String oldValue;
    private String newValue;
    private String note;
    private Instant createdAt;

    public static AuditDto from(TicketAuditEvent e) {
        return AuditDto.builder()
                .id(e.getId())
                .actor(e.getActor())
                .eventType(e.getEventType())
                .oldValue(e.getOldValue())
                .newValue(e.getNewValue())
                .note(e.getNote())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
