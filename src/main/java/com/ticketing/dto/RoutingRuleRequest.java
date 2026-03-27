package com.ticketing.dto;
import com.ticketing.domain.RoutingRule;
import lombok.Data;
@Data
public class RoutingRuleRequest {
    private String name;
    private RoutingRule.RuleType ruleType;
    private String matchValue;
    private RoutingRule.MatchField matchField;
    private String targetTeam;
    private String targetAgentId;
    private int priority;
    private boolean active;
}
