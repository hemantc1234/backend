package com.ticketing.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "tickets")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public class Ticket extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(name = "sender_name")
    private String senderName;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default private Priority priority = Priority.MEDIUM;

    @Column(name = "assigned_to")      private String assignedTo;
    @Column(name = "assigned_agent_id", columnDefinition = "CHAR(36)") private String assignedAgentId;
    @Column(name = "mailbox_id",        columnDefinition = "CHAR(36)") private String mailboxId;

    @Builder.Default private boolean overdue = false;

    @Column(name = "due_date") private Instant dueDate;
    @Column(name = "first_replied_at") private Instant firstRepliedAt;
    @Column(name = "resolved_at")      private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC") @Builder.Default
    private List<TicketMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC") @Builder.Default
    private List<TicketAuditEvent> auditEvents = new ArrayList<>();

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public enum Status   { OPEN, IN_PROGRESS, ON_HOLD, CLOSED }
    public enum Priority { LOW, MEDIUM, HIGH, URGENT }
}
