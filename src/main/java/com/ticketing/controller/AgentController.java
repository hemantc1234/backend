package com.ticketing.controller;

import com.ticketing.domain.Agent;
import com.ticketing.dto.AgentRequest;
import com.ticketing.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentRepository agentRepo;
    private final PasswordEncoder passwordEncoder;

    /** List active agents — optionally filter by team */
    @GetMapping
    public List<Agent> list(@RequestParam(required = false) String team) {
        if (team != null && !team.isBlank()) {
            return agentRepo.findByTeamAndActiveTrue(team);
        }
        return agentRepo.findByActiveTrue();
    }

    /** Create a new agent (managers only) */
    @PostMapping
    public ResponseEntity<Agent> create(@RequestBody AgentRequest req) {
        if (agentRepo.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        Agent agent = Agent.builder()
                .fullName(req.getFullName())
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(
                        req.getPassword() != null ? req.getPassword() : "Agent@1234"))
                .team(req.getTeam())
                .role(req.getRole() != null ? req.getRole() : Agent.Role.AGENT)
                .active(true)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(agentRepo.save(agent));
    }

    /** Deactivate an agent — soft delete (managers only) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        Agent agent = agentRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Agent not found: " + id));
        agent.setActive(false);
        agentRepo.save(agent);
        return ResponseEntity.noContent().build();
    }

    /** Update agent team / role */
    @PatchMapping("/{id}")
    public Agent update(@PathVariable String id, @RequestBody AgentRequest req) {
        Agent agent = agentRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Agent not found: " + id));
        if (req.getTeam()     != null) agent.setTeam(req.getTeam());
        if (req.getRole()     != null) agent.setRole(req.getRole());
        if (req.getFullName() != null) agent.setFullName(req.getFullName());
        if (req.getEmail()    != null) agent.setEmail(req.getEmail());
        return agentRepo.save(agent);
    }
}
