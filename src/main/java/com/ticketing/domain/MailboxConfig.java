package com.ticketing.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Represents one Microsoft 365 mailbox monitored via Microsoft Graph API.
 *
 * Auth is handled at the app level (app.graph.*).
 * Each MailboxConfig just stores the mailbox user principal name (email address)
 * and optional folder to read from (defaults to Inbox).
 */
@Entity
@Table(name = "mailbox_configs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public class MailboxConfig extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * The Microsoft 365 mailbox address to read from.
     * Must be a user in the same tenant as the app registration.
     * e.g. support@yourcompany.com
     */
    @Column(name = "mailbox_address", nullable = false)
    private String mailboxAddress;

    /**
     * Mail folder to read from. Defaults to "Inbox".
     * Can also be a folder ID or well-known name like "SentItems".
     */
    @Column(name = "folder_name")
    @Builder.Default
    private String folderName = "Inbox";

    /** When false the mailbox is skipped during polling */
    @Builder.Default
    private boolean active = true;

    @Column(name = "default_team")
    private String defaultTeam;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
