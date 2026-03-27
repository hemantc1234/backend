package com.ticketing.service;

import com.microsoft.graph.models.Message;
import com.ticketing.dto.ParsedEmail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Converts a Microsoft Graph {@link Message} into a flat {@link ParsedEmail} DTO.
 *
 * Key field mappings:
 *   Graph internetMessageId → Message-ID header (used for dedup + threading)
 *   Graph conversationId    → groups replies (backup threading key)
 *   Graph bodyPreview / body.content → email body text
 *   Graph from.emailAddress → sender
 */
@Service
@Slf4j
public class EmailParserService {

    public ParsedEmail parse(Message msg, String mailboxId) {
        String senderEmail = null;
        String senderName  = null;

        if (msg.getFrom() != null && msg.getFrom().getEmailAddress() != null) {
            senderEmail = msg.getFrom().getEmailAddress().getAddress();
            senderName  = msg.getFrom().getEmailAddress().getName();
        }

        // Graph uses internetMessageId as the RFC 2822 Message-ID header value
        String messageId = msg.getInternetMessageId();

        // Extract In-Reply-To from internet message headers if present
        String inReplyTo = extractHeader(msg, "In-Reply-To");

        // Prefer plain text body; fall back to HTML stripped of tags
        String body = extractBody(msg);

        return ParsedEmail.builder()
                .messageId(messageId)
                .inReplyTo(inReplyTo)
                .subject(msg.getSubject())
                .body(body)
                .senderEmail(senderEmail)
                .senderName(senderName)
                .mailboxId(mailboxId)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractBody(Message msg) {
        if (msg.getBody() == null) {
            // Fall back to preview (first 255 chars)
            return msg.getBodyPreview();
        }
        String content = msg.getBody().getContent();
        if (content == null) return msg.getBodyPreview();

        // If HTML, strip tags
        com.microsoft.graph.models.BodyType type = msg.getBody().getContentType();
        if (type == com.microsoft.graph.models.BodyType.Html) {
            return content
                    .replaceAll("(?s)<style[^>]*>.*?</style>", "")
                    .replaceAll("(?s)<script[^>]*>.*?</script>", "")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("&amp;", "&")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .replaceAll("\\s{2,}", " ")
                    .trim();
        }
        return content;
    }

    private String extractHeader(Message msg, String headerName) {
        if (msg.getInternetMessageHeaders() == null) return null;
        return msg.getInternetMessageHeaders().stream()
                .filter(h -> headerName.equalsIgnoreCase(h.getName()))
                .map(h -> h.getValue())
                .findFirst()
                .orElse(null);
    }
}
