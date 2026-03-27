package com.ticketing.service;

import com.ticketing.domain.RoutingRule;
import com.ticketing.dto.ParsedEmail;
import com.ticketing.repository.RoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingEngineService {

    private final RoutingRuleRepository ruleRepo;

    public String assign(ParsedEmail email) {
        List<RoutingRule> rules = ruleRepo.findByActiveTrueOrderByPriorityAsc();
        for (RoutingRule rule : rules) {
            if (matches(rule, email)) {
                log.debug("Rule '{}' matched for {}", rule.getName(), email.getSenderEmail());
                return rule.getTargetTeam() != null ? rule.getTargetTeam() : "General Queue";
            }
        }
        return "General Queue";
    }

    public Optional<String> resolveAgentId(ParsedEmail email) {
        return ruleRepo.findByActiveTrueOrderByPriorityAsc().stream()
                .filter(r -> matches(r, email) && r.getTargetAgentId() != null)
                .map(RoutingRule::getTargetAgentId)
                .findFirst();
    }

    private boolean matches(RoutingRule rule, ParsedEmail email) {
        RoutingRule.RuleType type = rule.getRuleType();

        if (type == RoutingRule.RuleType.DEFAULT) {
            return true;
        }

        if (type == RoutingRule.RuleType.KEYWORD) {
            if (rule.getMatchValue() == null) return false;
            String hay = haystack(rule.getMatchField(), email);
            return Arrays.stream(rule.getMatchValue().split(","))
                    .map(String::trim)
                    .anyMatch(kw -> hay.toLowerCase().contains(kw.toLowerCase()));
        }

        if (type == RoutingRule.RuleType.DOMAIN) {
            return rule.getMatchValue() != null
                    && email.getSenderEmail() != null
                    && email.getSenderEmail().toLowerCase()
                             .endsWith(rule.getMatchValue().trim().toLowerCase());
        }

        if (type == RoutingRule.RuleType.VIP) {
            return rule.getMatchValue() != null
                    && rule.getMatchValue().trim().equalsIgnoreCase(email.getSenderEmail());
        }

        return false;
    }

    private String haystack(RoutingRule.MatchField field, ParsedEmail email) {
        if (field == RoutingRule.MatchField.SUBJECT)         return s(email.getSubject());
        if (field == RoutingRule.MatchField.BODY)            return s(email.getBody());
        if (field == RoutingRule.MatchField.SUBJECT_OR_BODY) return s(email.getSubject()) + " " + s(email.getBody());
        if (field == RoutingRule.MatchField.SENDER)          return s(email.getSenderEmail());
        return "";
    }

    private String s(String v) { return v != null ? v : ""; }
}
