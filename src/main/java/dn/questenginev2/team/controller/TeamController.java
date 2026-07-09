package dn.questenginev2.team.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.team.dto.*;
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
@RequestMapping(Routes.TEAMS)
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request, Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(teamService.createTeam(request, auth));
    }

    @PostMapping(Routes.TEAM_ID_JOIN_REQUEST)
    public ResponseEntity<Boolean> sendJoinRequest(@PathVariable Long teamId, @RequestParam(required = false) String username, Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(teamService.createJoinRequest(auth, teamId, username));
    }

    @PostMapping(Routes.APPROVE_JOIN_REQUEST)
    public ResponseEntity<Boolean> approveRequest(@PathVariable Long requestId, Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamService.approveRequest(requestId, auth));
    }

    @DeleteMapping(Routes.LEAVE)
    public ResponseEntity<Boolean> leaveTeam(Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamService.leaveTeam(auth));
    }

    @PostMapping(Routes.REJECT_JOIN_REQUEST)
    public ResponseEntity<Boolean> rejectRequest(@PathVariable Long requestId, Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamService.rejectRequest(requestId, auth));
    }

    @PostMapping(Routes.TRANSFER_CAPTAIN)
    public ResponseEntity<Boolean> transferCaptain(@PathVariable Long userId, Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamService.transferCaptain(userId, auth));
    }

    @GetMapping(Routes.TEAM_ID)
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long teamId, Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamService.getTeamById(teamId));
    }

    @GetMapping(Routes.JOIN_REQUESTS)
    public ResponseEntity<List<TeamJoinResponse>> getJoinRequests(Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamService.getJoinRequests(auth));
    }

    @GetMapping(Routes.MY)
    public ResponseEntity<TeamResponse> getMyTeam(Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamService.getMyTeam(auth));
    }

    @GetMapping(Routes.MEMBERS)
    public ResponseEntity<List<TeamMemberDto>> getTeamMembers(@PathVariable Long teamId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(teamService.getTeamMembers(teamId));
    }
}
