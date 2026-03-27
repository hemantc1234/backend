package com.ticketing.repository;
import com.ticketing.domain.SlaPolicy;
import com.ticketing.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, String> {
    Optional<SlaPolicy> findByPriority(Ticket.Priority priority);
}
