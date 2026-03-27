package com.ticketing.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "agents")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public class Agent extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.AGENT;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum Role { AGENT, SENIOR_AGENT, MANAGER }
}
