package com.ticketing.controller;

import com.ticketing.dto.TeamSummaryDto;
import com.ticketing.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public List<TeamSummaryDto> list() {
        return teamService.getAllTeams();
    }
}
