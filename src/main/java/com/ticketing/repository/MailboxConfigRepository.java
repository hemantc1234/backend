package com.ticketing.repository;
import com.ticketing.domain.MailboxConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MailboxConfigRepository extends JpaRepository<MailboxConfig, String> {
    List<MailboxConfig> findByActiveTrue();
}
