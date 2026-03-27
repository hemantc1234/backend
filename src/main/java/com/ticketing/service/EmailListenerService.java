package com.ticketing.service;

import com.microsoft.graph.models.Message;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.ticketing.domain.MailboxConfig;
import com.ticketing.domain.ProcessedGraphMessage;
import com.ticketing.repository.MailboxConfigRepository;
import com.ticketing.repository.ProcessedGraphMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Polls Microsoft 365 mailboxes via Microsoft Graph API.
 *
 * Requires only Mail.Read application permission (not Mail.ReadWrite).
 * Instead of marking messages as read in Graph, we track processed
 * internetMessageId values in the processed_graph_messages table.
 *
 * The $filter fetches the last 50 unread messages. Already-processed
 * ones are skipped immediately via the DB lookup.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailListenerService {

    private final GraphServiceClient                graphClient;
    private final MailboxConfigRepository           mailboxRepo;
    private final ProcessedGraphMessageRepository   processedRepo;
    private final EmailParserService                parser;
    private final SpamFilterService                 spamFilter;
    private final TicketService                     ticketService;

    @Scheduled(fixedDelayString = "${app.graph.poll-interval-ms:60000}")
    public void pollAllMailboxes() {
        List<MailboxConfig> active = mailboxRepo.findByActiveTrue();
        log.info("Graph poll — {} active mailbox(es)", active.size());
        active.forEach(this::pollAsync);

        // Purge processed IDs older than 30 days to keep table lean
        processedRepo.deleteOlderThan(Instant.now().minus(30, ChronoUnit.DAYS));
    }

    @Async
    public void pollAsync(MailboxConfig mb) {
        log.debug("Polling mailbox via Graph: {}", mb.getMailboxAddress());
        try {
            var response = graphClient
                    .users()
                    .byUserId(mb.getMailboxAddress())
                    .mailFolders()
                    .byMailFolderId(mb.getFolderName())
                    .messages()
                    .get(req -> {
                        req.queryParameters.filter = "isRead eq false";
                        req.queryParameters.top    = 50;
                        req.queryParameters.select = new String[]{
                            "id", "subject", "from", "bodyPreview", "body",
                            "internetMessageId", "internetMessageHeaders",
                            "conversationId", "receivedDateTime"
                        };
                        // NOTE: body is a regular property — do NOT add it to $expand
                    });

            if (response == null || response.getValue() == null) return;

            List<Message> messages = response.getValue();
            log.info("Mailbox [{}] — {} unread message(s) fetched", mb.getMailboxAddress(), messages.size());

            for (Message msg : messages) {
                processMessage(msg, mb);
            }

        } catch (Exception e) {
            log.error("Failed to poll mailbox [{}]: {}", mb.getMailboxAddress(), e.getMessage(), e);
        }
    }

    private void processMessage(Message msg, MailboxConfig mb) {
        String internetMsgId = msg.getInternetMessageId();

        // Skip if we've already processed this message
        if (internetMsgId != null && processedRepo.existsByMessageId(internetMsgId)) {
            log.debug("Already processed, skipping: {}", internetMsgId);
            return;
        }

        try {
            var email = parser.parse(msg, mb.getId());

            // Spam filter
            var filter = spamFilter.checkGraph(msg, email.getSubject(), email.getSenderEmail());
            if (filter.blocked()) {
                log.info("Spam filtered ({}): {}", filter.reason(), email.getSenderEmail());
            } else {
                // Hand off to ticket pipeline
                ticketService.processAsync(email);
            }

            // Mark as processed in DB regardless of spam result
            // so we don't attempt to reprocess on the next poll
            if (internetMsgId != null) {
                processedRepo.save(ProcessedGraphMessage.builder()
                        .messageId(internetMsgId)
                        .mailboxId(mb.getId())
                        .build());
            }

        } catch (Exception e) {
            log.error("Error processing Graph message: {}", e.getMessage(), e);
        }
    }
}
