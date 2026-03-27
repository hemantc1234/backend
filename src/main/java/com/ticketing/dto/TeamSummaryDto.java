package com.ticketing.dto;

import com.ticketing.domain.Agent;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeamSummaryDto {
    private String name;
    private long   openTickets;
    private long   inProgressTickets;
    private long   closedTickets;
    private long   overdueTickets;
    private List<AgentSummary> agents;

    @Data
    @Builder
    public static class AgentSummary {
        private String id;
        private String username;
        private String fullName;
        private String email;
        private Agent.Role role;
        private boolean active;
    }
}
