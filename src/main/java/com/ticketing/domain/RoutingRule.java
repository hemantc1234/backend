package com.ticketing.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "routing_rules")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper = true) @ToString(callSuper = true)
public class RoutingRule extends BaseEntity {


    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    @Column(name = "match_value", length = 500)
    private String matchValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_field", nullable = false)
    @Builder.Default
    private MatchField matchField = MatchField.SUBJECT_OR_BODY;

    @Column(name = "target_team")
    private String targetTeam;

    @Column(name = "target_agent_id", columnDefinition = "CHAR(36)")
    private String targetAgentId;

    @Builder.Default
    private int priority = 100;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum RuleType   { KEYWORD, DOMAIN, VIP, DEFAULT }
    public enum MatchField { SUBJECT, BODY, SUBJECT_OR_BODY, SENDER }
}
