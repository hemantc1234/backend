package com.ticketing.controller;

import com.ticketing.config.JwtService;
import com.ticketing.dto.*;
import com.ticketing.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService            jwtService;
    private final AuthenticationManager authManager;
    private final AgentRepository       agentRepo;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        var agent = agentRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Agent not found"));

        return LoginResponse.builder()
                .token(jwtService.generate(agent.getUsername(), agent.getRole().name()))
                .username(agent.getUsername())
                .role(agent.getRole().name())
                .fullName(agent.getFullName())
                .build();
    }
}
