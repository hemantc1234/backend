package com.ticketing.service;

import com.ticketing.domain.Ticket;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailer;

    @Value("${app.mail.from-address}") private String fromAddress;
    @Value("${app.mail.from-name}")    private String fromName;

    // ── Auto-acknowledgement on ticket creation ───────────────────────────────
    @Async
    public void sendAck(Ticket ticket) {
        String ref      = shortRef(ticket);
        String greeting = ticket.getSenderName() != null ? ticket.getSenderName() : ticket.getSenderEmail();
        String html = """
                <html><body style="font-family:sans-serif;color:#333;line-height:1.6">
                <p>Hi %s,</p>
                <p>We have received your request and assigned it reference <strong>#%s</strong>.</p>
                <p>Our team will respond as soon as possible.</p>
                <p style="color:#888;font-size:12px">Please keep the subject line unchanged when replying.</p>
                </body></html>""".formatted(greeting, ref);
        send(ticket.getSenderEmail(),
             "Your request has been received [Ticket #" + ref + "]",
             html);
    }

    // ── Manual reply from agent to customer ───────────────────────────────────
    @Async
    public void sendReplyToCustomer(Ticket ticket, String subject, String body, String agentEmail) {
        String ref = shortRef(ticket);

        // Build a clean HTML email with a reference footer
        String html = """
                <html><body style="font-family:sans-serif;color:#333;line-height:1.6">
                <p>%s</p>
                <hr style="border:none;border-top:1px solid #eee;margin:24px 0"/>
                <p style="color:#888;font-size:12px">
                  Reference: <strong>#%s</strong><br/>
                  Please keep the subject line unchanged when replying so we can
                  link your message to the same ticket.
                </p>
                </body></html>""".formatted(
                    body.replace("\n", "<br/>"),
                    ref
                );

        String emailSubject = subject != null && !subject.isBlank()
                ? subject
                : "Re: " + ticket.getSubject() + " [#" + ref + "]";

        send(ticket.getSenderEmail(), emailSubject, html);
        log.info("Reply email sent to {} for ticket {} by {}", ticket.getSenderEmail(), ticket.getId(), agentEmail);
    }

    // ── SLA breach alert ──────────────────────────────────────────────────────
    @Async
    public void sendSlaAlert(Ticket ticket) {
        log.warn("SLA breached — ticket {} priority {}", ticket.getId(), ticket.getPriority());
        // In production: email the manager team or post to a Slack webhook
    }

    // ── Shared send helper ────────────────────────────────────────────────────
    private void send(String to, String subject, String html) {
        try {
            MimeMessage msg = mailer.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, false, "UTF-8");
            h.setFrom(fromAddress, fromName);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            mailer.send(msg);
            log.debug("Email sent → {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String shortRef(Ticket ticket) {
        return ticket.getId().substring(0, 8).toUpperCase();
    }
}
