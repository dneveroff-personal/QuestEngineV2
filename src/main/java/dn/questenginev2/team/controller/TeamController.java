package dn.questenginev2.team.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.dto.TeamFilterRequest;
import dn.questenginev2.team.dto.TeamJoinResponse;
import dn.questenginev2.team.dto.TeamMemberDto;
import dn.questenginev2.team.dto.TeamResponse;
import dn.questenginev2.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.TEAMS)
@Tag(name = "Teams", description = "Team management endpoints")
public class TeamController {

  private final TeamService teamService;

  @Operation(summary = "Create team", description = "Create a new team")
  @PostMapping
  public ResponseEntity<TeamResponse> create(
      @Valid @RequestBody CreateTeamRequest request, Authentication auth) {
    return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request, auth));
  }

  @Operation(summary = "Send join request", description = "Send a request to join a team")
  @PostMapping(Routes.TEAM_ID_JOIN_REQUEST)
  public ResponseEntity<Boolean> sendJoinRequest(
      @PathVariable Long teamId,
      @RequestParam(required = false) String username,
      Authentication auth) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(teamService.createJoinRequest(auth, teamId, username));
  }

  @Operation(summary = "Approve join request", description = "Approve a team join request")
  @PostMapping(Routes.APPROVE_JOIN_REQUEST)
  public ResponseEntity<Boolean> approveRequest(@PathVariable Long requestId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK).body(teamService.approveRequest(requestId, auth));
  }

  @Operation(summary = "Leave team", description = "Leave current team")
  @DeleteMapping(Routes.LEAVE)
  public ResponseEntity<Boolean> leaveTeam(Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK).body(teamService.leaveTeam(auth));
  }

  @Operation(summary = "Reject join request", description = "Reject a team join request")
  @PostMapping(Routes.REJECT_JOIN_REQUEST)
  public ResponseEntity<Boolean> rejectRequest(@PathVariable Long requestId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK).body(teamService.rejectRequest(requestId, auth));
  }

  @Operation(
      summary = "Transfer captain",
      description = "Transfer team captain role to another user")
  @PostMapping(Routes.TRANSFER_CAPTAIN)
  public ResponseEntity<Boolean> transferCaptain(@PathVariable Long userId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK).body(teamService.transferCaptain(userId, auth));
  }

  @Operation(summary = "Get team by ID", description = "Retrieve team details by ID")
  @GetMapping(Routes.TEAM_ID)
  public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long teamId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK).body(teamService.getTeamById(teamId));
  }

  @Operation(
      summary = "Get join requests",
      description = "Get all join requests for current user's team")
  @GetMapping(Routes.JOIN_REQUESTS)
  public ResponseEntity<List<TeamJoinResponse>> getJoinRequests(Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK).body(teamService.getJoinRequests(auth));
  }

  @Operation(summary = "Get my team", description = "Get current user's team")
  @GetMapping(Routes.MY)
  public ResponseEntity<TeamResponse> getMyTeam(Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK).body(teamService.getMyTeam(auth));
  }

  @Operation(summary = "Get team members", description = "Get all members of a team")
  @GetMapping(Routes.MEMBERS)
  public ResponseEntity<List<TeamMemberDto>> getTeamMembers(@PathVariable Long teamId) {
    return ResponseEntity.status(HttpStatus.OK).body(teamService.getTeamMembers(teamId));
  }

  @Operation(
      summary = "Search teams",
      description = "Search teams with dynamic filters (name, captain, date range)")
  @GetMapping("/search")
  public ResponseEntity<List<TeamResponse>> searchTeams(
      @Valid TeamFilterRequest filter, Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(teamService.searchTeams(filter, pageable));
  }
}
