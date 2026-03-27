package com.ticketing.controller;

import com.ticketing.domain.RoutingRule;
import com.ticketing.dto.RoutingRuleRequest;
import com.ticketing.repository.RoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/routing-rules")
@RequiredArgsConstructor
public class RoutingRuleController {

    private final RoutingRuleRepository ruleRepo;

    @GetMapping
    public List<RoutingRule> list() {
        return ruleRepo.findAll(Sort.by("priority"));
    }

    @PostMapping
    public ResponseEntity<RoutingRule> create(@RequestBody RoutingRuleRequest req) {
        RoutingRule rule = RoutingRule.builder()
                .name(req.getName()).ruleType(req.getRuleType())
                .matchValue(req.getMatchValue()).matchField(req.getMatchField())
                .targetTeam(req.getTargetTeam()).targetAgentId(req.getTargetAgentId())
                .priority(req.getPriority()).active(req.isActive()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleRepo.save(rule));
    }

    @PutMapping("/{id}")
    public RoutingRule update(@PathVariable String id, @RequestBody RoutingRuleRequest req) {
        RoutingRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Rule not found: " + id));
        rule.setName(req.getName()); rule.setRuleType(req.getRuleType());
        rule.setMatchValue(req.getMatchValue()); rule.setMatchField(req.getMatchField());
        rule.setTargetTeam(req.getTargetTeam()); rule.setPriority(req.getPriority());
        rule.setActive(req.isActive());
        return ruleRepo.save(rule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        ruleRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
