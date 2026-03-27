package com.ticketing.repository;
import com.ticketing.domain.SpamFilterLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SpamFilterLogRepository extends JpaRepository<SpamFilterLog, String> {}
