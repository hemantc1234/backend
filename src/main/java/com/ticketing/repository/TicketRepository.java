package com.ticketing.repository;

import com.ticketing.domain.Ticket;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface TicketRepository extends JpaRepository<Ticket, String> {

    long countByStatus(Ticket.Status status);
    long countByOverdueTrue();
    long countByAssignedToAndStatus(String assignedTo, Ticket.Status status);
    long countByAssignedToAndOverdueTrue(String assignedTo);

    // Stage counts
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status IN ('OPEN','IN_PROGRESS','ON_HOLD')")
    long countUnresolved();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.dueDate BETWEEN :start AND :end AND t.status <> 'CLOSED'")
    long countDueToday(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo IS NULL AND t.status <> 'CLOSED'")
    long countUnassigned();

    @Query("SELECT t FROM Ticket t WHERE t.status <> 'CLOSED' AND t.overdue = false")
    List<Ticket> findOpenNonOverdue();

    // Advanced search with all filters
    @Query("""
           SELECT t FROM Ticket t WHERE
               (:stage      IS NULL OR (
                   :stage = 'UNRESOLVED' AND t.status IN ('OPEN','IN_PROGRESS','ON_HOLD') OR
                   :stage = 'OVERDUE'    AND t.overdue = true OR
                   :stage = 'DUE_TODAY' AND t.dueDate BETWEEN :dayStart AND :dayEnd AND t.status <> 'CLOSED' OR
                   :stage = 'OPEN'      AND t.status = 'OPEN' OR
                   :stage = 'ON_HOLD'   AND t.status = 'ON_HOLD' OR
                   :stage = 'UNASSIGNED' AND t.assignedTo IS NULL AND t.status <> 'CLOSED'
               )) AND
               (:status     IS NULL OR t.status     = :status)    AND
               (:priority   IS NULL OR t.priority   = :priority)  AND
               (:assignedTo IS NULL OR t.assignedTo = :assignedTo) AND
               (:agentId    IS NULL OR t.assignedAgentId = :agentId) AND
               (:createdFrom IS NULL OR t.createdAt >= :createdFrom) AND
               (:createdTo   IS NULL OR t.createdAt <= :createdTo)   AND
               (:dueFrom     IS NULL OR t.dueDate   >= :dueFrom)     AND
               (:dueTo       IS NULL OR t.dueDate   <= :dueTo)
           """)
    Page<Ticket> search(
            @Param("stage")       String stage,
            @Param("status")      Ticket.Status status,
            @Param("priority")    Ticket.Priority priority,
            @Param("assignedTo")  String assignedTo,
            @Param("agentId")     String agentId,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo")   Instant createdTo,
            @Param("dueFrom")     Instant dueFrom,
            @Param("dueTo")       Instant dueTo,
            @Param("dayStart")    Instant dayStart,
            @Param("dayEnd")      Instant dayEnd,
            Pageable pageable);
}
