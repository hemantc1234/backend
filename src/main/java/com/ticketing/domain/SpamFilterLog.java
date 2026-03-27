package com.ticketing.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "spam_filter_log")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public class SpamFilterLog extends BaseEntity {


    @Column(name = "message_id", length = 500)
    private String messageId;

    @Column(name = "sender_email")
    private String senderEmail;

    @Column(length = 500)
    private String subject;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
