package com.ticketing.repository;

import com.ticketing.domain.Ticket;
import com.ticketing.domain.TicketMessage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, String> {
    boolean existsByMessageId(String messageId);

    @Query("SELECT m.ticket FROM TicketMessage m WHERE m.messageId = :inReplyTo")
    Optional<Ticket> findTicketByInReplyTo(@Param("inReplyTo") String inReplyTo);

    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(String ticketId);
}
