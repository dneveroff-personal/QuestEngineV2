package dn.questenginev2.team.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.dto.TeamJoinResponse;
import dn.questenginev2.team.dto.TeamResponse;
import dn.questenginev2.team.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.API)
public class TeamController {

    private final TeamService teamService;

    @PostMapping(Routes.TEAMS)
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request, Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(teamService.createTeam(request, auth));
    }

    @PostMapping(Routes.TEAM_ID_JOIN_REQUEST)
    public ResponseEntity<Boolean> sendJoinRequest(@PathVariable Long teamId, Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(teamService.createJoinRequest(auth, teamId));
    }

    @GetMapping(Routes.JOIN_REQUESTS)
    public ResponseEntity<List<TeamJoinResponse>> getJoinRequests(Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamService.getJoinRequests(auth));
    }

    @PostMapping(Routes.APPROVE_JOIN_REQUEST)
    public ResponseEntity<Boolean> approveRequest(@PathVariable Long requestId, Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamService.approveRequest(requestId, auth));
    }
}
