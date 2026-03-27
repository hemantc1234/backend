package com.ticketing.config;

import com.ticketing.domain.Agent;
import com.ticketing.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AgentRepository agentRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        // Check by username — V3 demo data seeds other agents so count() > 0
        // but the admin might still be absent if starting fresh without demo data
        if (agentRepo.findByUsername("admin").isEmpty()) {
            agentRepo.save(Agent.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .fullName("System Administrator")
                    .password(passwordEncoder.encode("Admin@1234"))
                    .team("Management")
                    .role(Agent.Role.MANAGER)
                    .active(true)
                    .build());
            log.info(">>> Default admin created  username=admin  password=Admin@1234");
        }
    }
}
