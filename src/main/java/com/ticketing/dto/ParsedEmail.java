package com.ticketing.dto;

import com.ticketing.domain.*;
import lombok.*;
import java.time.Instant;
import java.util.List;

// ── ParsedEmail (internal pipeline) ──────────────────────────────────────────
@Data @Builder
public class ParsedEmail {
    private String messageId;
    private String inReplyTo;
    private String subject;
    private String body;
    private String senderEmail;
    private String senderName;
    private String mailboxId;
}
