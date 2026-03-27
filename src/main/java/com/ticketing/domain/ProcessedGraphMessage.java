package com.ticketing.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "processed_graph_messages")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProcessedGraphMessage {

    @Id
    @Column(name = "message_id", length = 500)
    private String messageId;

    @Column(name = "mailbox_id", nullable = false, columnDefinition = "CHAR(36)")
    private String mailboxId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant processedAt = Instant.now();
}
