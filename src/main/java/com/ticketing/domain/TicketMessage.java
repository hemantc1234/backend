package com.ticketing.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "ticket_messages")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public class TicketMessage extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "message_id", nullable = false, unique = true, length = 500)
    private String messageId;

    @Column(name = "in_reply_to", length = 500)
    private String inReplyTo;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String body;

    @Column(name = "sender_email")
    private String senderEmail;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Direction direction = Direction.INBOUND;

    @Builder.Default
    private boolean internal = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum Direction { INBOUND, OUTBOUND }
}
