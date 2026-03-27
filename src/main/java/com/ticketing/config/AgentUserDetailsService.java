package com.ticketing.config;

import com.ticketing.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentUserDetailsService implements UserDetailsService {

    private final AgentRepository agentRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var agent = agentRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Agent not found: " + username));
        return User.withUsername(agent.getUsername())
                .password(agent.getPassword())
                .roles(agent.getRole().name())
                .disabled(!agent.isActive())
                .build();
    }
}
