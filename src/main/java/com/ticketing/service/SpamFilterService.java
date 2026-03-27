package com.ticketing.service;

import com.microsoft.graph.models.InternetMessageHeader;
import com.microsoft.graph.models.Message;
import com.ticketing.domain.SpamFilterLog;
import com.ticketing.repository.SpamFilterLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Detects auto-replies, bounces, and spam before ticket creation.
 * Works with Microsoft Graph Message objects.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpamFilterService {

    private final SpamFilterLogRepository spamLogRepo;

    private static final Set<String> AUTO_REPLY_HEADERS = Set.of(
            "X-Autoreply", "Auto-Submitted", "X-Auto-Response-Suppress"
    );

    private static final Set<String> SUBJECT_BLOCKLIST = Set.of(
            "out of office", "automatic reply", "auto reply", "auto-reply",
            "undeliverable", "delivery failure", "mailer-daemon",
            "mail delivery failed", "returned mail"
    );

    public record FilterResult(boolean blocked, String reason) {}

    /** Check a Microsoft Graph message for spam/auto-reply signals */
    public FilterResult checkGraph(Message msg, String subject, String senderEmail) {
        List<InternetMessageHeader> headers =
                msg.getInternetMessageHeaders() != null ? msg.getInternetMessageHeaders() : List.of();

        // Check auto-reply internet headers
        for (String headerName : AUTO_REPLY_HEADERS) {
            String val = getHeader(headers, headerName);
            if (val != null && !val.equalsIgnoreCase("no")) {
                return block(senderEmail, subject, headerName + "=" + val);
            }
        }

        // Precedence: bulk / junk / list
        String precedence = getHeader(headers, "Precedence");
        if (precedence != null && precedence.matches("(?i)bulk|junk|list")) {
            return block(senderEmail, subject, "Precedence=" + precedence);
        }

        // Mailer-daemon sender
        if (senderEmail != null && senderEmail.toLowerCase().startsWith("mailer-daemon")) {
            return block(senderEmail, subject, "Mailer-daemon sender");
        }

        // Subject keyword blocklist
        if (subject != null) {
            String lc = subject.toLowerCase();
            for (String kw : SUBJECT_BLOCKLIST) {
                if (lc.contains(kw)) return block(senderEmail, subject, "Blocked subject: " + kw);
            }
        }

        return new FilterResult(false, null);
    }

    private String getHeader(List<InternetMessageHeader> headers, String name) {
        return headers.stream()
                .filter(h -> name.equalsIgnoreCase(h.getName()))
                .map(InternetMessageHeader::getValue)
                .findFirst()
                .orElse(null);
    }

    private FilterResult block(String sender, String subject, String reason) {
        log.info("Spam blocked: {} | sender: {}", reason, sender);
        spamLogRepo.save(SpamFilterLog.builder()
                .senderEmail(sender).subject(subject).reason(reason).build());
        return new FilterResult(true, reason);
    }
}
