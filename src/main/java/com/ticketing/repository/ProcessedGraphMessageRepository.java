package com.ticketing.repository;

import com.ticketing.domain.ProcessedGraphMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface ProcessedGraphMessageRepository extends JpaRepository<ProcessedGraphMessage, String> {

    boolean existsByMessageId(String messageId);

    /** Purge old records to keep the table lean — keep last 30 days */
    @Modifying
    @Transactional
    @Query("DELETE FROM ProcessedGraphMessage m WHERE m.processedAt < :cutoff")
    void deleteOlderThan(Instant cutoff);
}
