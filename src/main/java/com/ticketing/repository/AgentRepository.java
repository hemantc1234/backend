package com.ticketing.repository;
import com.ticketing.domain.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AgentRepository extends JpaRepository<Agent, String> {
    Optional<Agent> findByUsername(String username);
    List<Agent> findByActiveTrue();
    List<Agent> findByTeamAndActiveTrue(String team);
}
