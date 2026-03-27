package com.ticketing.service;

import com.ticketing.domain.Ticket;
import com.ticketing.repository.TicketMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ThreadingService {

    private final TicketMessageRepository messageRepo;

    public Optional<Ticket> findParentTicket(String inReplyTo) {
        if (inReplyTo == null || inReplyTo.isBlank()) return Optional.empty();
        return messageRepo.findTicketByInReplyTo(inReplyTo.trim());
    }

    public boolean isDuplicate(String messageId) {
        if (messageId == null || messageId.isBlank()) return false;
        return messageRepo.existsByMessageId(messageId.trim());
    }
}
