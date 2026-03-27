package com.ticketing.dto;

import com.ticketing.domain.Agent;
import lombok.Data;

@Data
public class AgentRequest {
    private String     fullName;
    private String     username;
    private String     email;
    private String     password;
    private String     team;
    private Agent.Role role;
}
