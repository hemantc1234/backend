package com.ticketing.service;

import com.ticketing.domain.*;
import com.ticketing.dto.TeamSummaryDto;
import com.ticketing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final AgentRepository       agentRepo;
    private final RoutingRuleRepository ruleRepo;
    private final TicketRepository      ticketRepo;

    @Transactional(readOnly = true)
    public List<TeamSummaryDto> getAllTeams() {

        // 1. Collect all known team names from routing rules
        Set<String> teamNames = ruleRepo.findAll().stream()
                .map(RoutingRule::getTargetTeam)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        // 2. Also include any team names that appear on agents but not in rules
        agentRepo.findAll().stream()
                .map(Agent::getTeam)
                .filter(Objects::nonNull)
                .forEach(teamNames::add);

        // 3. Group agents by team
        Map<String, List<Agent>> agentsByTeam = agentRepo.findAll().stream()
                .filter(a -> a.getTeam() != null)
                .collect(Collectors.groupingBy(Agent::getTeam));

        // 4. For each team, count tickets by status
        return teamNames.stream()
                .sorted()
                .map(team -> {
                    List<Agent> members = agentsByTeam.getOrDefault(team, List.of());

                    return TeamSummaryDto.builder()
                            .name(team)
                            .openTickets(      countTickets(team, Ticket.Status.OPEN))
                            .inProgressTickets(countTickets(team, Ticket.Status.IN_PROGRESS))
                            .closedTickets(    countTickets(team, Ticket.Status.CLOSED))
                            .overdueTickets(   countOverdue(team))
                            .agents(members.stream().map(this::toAgentSummary).toList())
                            .build();
                })
                .toList();
    }

    private long countTickets(String team, Ticket.Status status) {
        return ticketRepo.countByAssignedToAndStatus(team, status);
    }

    private long countOverdue(String team) {
        return ticketRepo.countByAssignedToAndOverdueTrue(team);
    }

    private TeamSummaryDto.AgentSummary toAgentSummary(Agent a) {
        return TeamSummaryDto.AgentSummary.builder()
                .id(a.getId())
                .username(a.getUsername())
                .fullName(a.getFullName())
                .email(a.getEmail())
                .role(a.getRole())
                .active(a.isActive())
                .build();
    }
}
