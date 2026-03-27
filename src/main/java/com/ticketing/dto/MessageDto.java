package com.ticketing.dto;

import com.ticketing.domain.TicketMessage;
import lombok.*;
import java.time.Instant;

@Data @Builder
public class MessageDto {
    private String id;
    private String body;
    private String senderEmail;
    private TicketMessage.Direction direction;
    private boolean internal;
    private Instant createdAt;

    public static MessageDto from(TicketMessage m) {
        return MessageDto.builder()
                .id(m.getId())
                .body(m.getBody())
                .senderEmail(m.getSenderEmail())
                .direction(m.getDirection())
                .internal(m.isInternal())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
