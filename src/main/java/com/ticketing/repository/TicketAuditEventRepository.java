package com.ticketing.repository;
import com.ticketing.domain.TicketAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TicketAuditEventRepository extends JpaRepository<TicketAuditEvent, String> {
    List<TicketAuditEvent> findByTicketIdOrderByCreatedAtDesc(String ticketId);
}
