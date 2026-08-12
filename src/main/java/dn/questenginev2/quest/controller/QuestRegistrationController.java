package dn.questenginev2.quest.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.quest.dto.QuestRegisterResponse;
import dn.questenginev2.quest.service.QuestRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.QUEST_REGISTER)
@Tag(name = "Quest Registrations", description = "Quest registrations endpoints")
public class QuestRegistrationController {

  private final QuestRegistrationService questRegisterService;

  @Operation(summary = "Add quest registration", description = "Register new team on quest")
  @PostMapping(Routes.QUEST_ID + Routes.TEAM_ID)
  public ResponseEntity<QuestRegisterResponse> register(
      @PathVariable Long questId, @PathVariable Long teamId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(questRegisterService.registerTeam(questId, teamId, auth));
  }

  @Operation(
      summary = "Get all registered teams",
      description = "Retrieve all teams registered on quest")
  @GetMapping(Routes.QUEST_ID)
  public ResponseEntity<List<QuestRegisterResponse>> getRegisteredTeams(
      @PathVariable Long questId) {
    return ResponseEntity.status(HttpStatus.OK).body(questRegisterService.findAll(questId));
  }

  @Operation(summary = "Unregister team", description = "Delete team registration on quest")
  @DeleteMapping(Routes.QUEST_ID)
  public ResponseEntity<QuestRegisterResponse> unregisterTeam(
      @PathVariable Long questId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(questRegisterService.unregisterTeam(questId, auth));
  }

  @Operation(
      summary = "Approve team for quest",
      description = "Author approve team to participate in quest")
  @PutMapping(Routes.QUEST_ID + "/approve" + Routes.TEAM_ID)
  public ResponseEntity<QuestRegisterResponse> approveTeam(
      @PathVariable Long questId, @PathVariable Long teamId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(questRegisterService.approveTeam(questId, teamId, auth));
  }

  @Operation(
      summary = "Reject team for quest",
      description = "Author rejected to team to participate in quest")
  @PutMapping(Routes.QUEST_ID + "/teams" + Routes.TEAM_ID + "/reject")
  public ResponseEntity<QuestRegisterResponse> rejectTeam(
      @PathVariable Long questId, @PathVariable Long teamId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(questRegisterService.rejectTeam(teamId, questId, auth));
  }
}
