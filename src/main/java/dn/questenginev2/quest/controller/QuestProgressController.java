package dn.questenginev2.quest.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.quest.dto.QuestProgressResponse;
import dn.questenginev2.quest.service.QuestProgressService;
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
@RequestMapping(Routes.QUEST_PROGRESS)
@Tag(name = "Quest Progress", description = "Quest progress endpoints")
public class QuestProgressController {

  private final QuestProgressService questProgressService;

  @Operation(summary = "Enter quest", description = "Team enters the quest (WAITING -> RUNNING)")
  @PostMapping(Routes.QUEST_ID + "/enter")
  public ResponseEntity<QuestProgressResponse> enterQuest(
      @PathVariable Long questId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(questProgressService.enterQuest(questId, auth));
  }

  @Operation(summary = "Get team progress", description = "Get progress for specific team on quest")
  @GetMapping(Routes.QUEST_ID + Routes.TEAM_ID)
  public ResponseEntity<QuestProgressResponse> getProgress(
      @PathVariable Long questId, @PathVariable Long teamId) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(questProgressService.getProgress(questId, teamId));
  }

  @Operation(
      summary = "Get all progress by quest",
      description = "Get all teams progress for specific quest")
  @GetMapping(Routes.QUEST_ID)
  public ResponseEntity<List<QuestProgressResponse>> getAllByQuest(@PathVariable Long questId) {
    return ResponseEntity.status(HttpStatus.OK).body(questProgressService.getAllByQuest(questId));
  }

  @Operation(
      summary = "Finish team progress",
      description = "Author finishes team progress (RUNNING -> FINISHED)")
  @PutMapping(Routes.QUEST_ID + Routes.TEAM_ID + "/finish")
  public ResponseEntity<QuestProgressResponse> finishProgress(
      @PathVariable Long questId, @PathVariable Long teamId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(questProgressService.finishProgress(questId, teamId, auth));
  }

  @Operation(
      summary = "Set DNF for team",
      description =
          "Manual DNF override, available only after Quest.status = FINISHED. All teams that did"
              + " not finish are already marked DNF automatically when the author finishes the"
              + " Quest; this endpoint additionally lets the author disqualify an already"
              + " FINISHED team retroactively (e.g. a confirmed rules violation discovered"
              + " during review) — see 01-domain/progress.md.")
  @PutMapping(Routes.QUEST_ID + Routes.TEAM_ID + "/dnf")
  public ResponseEntity<QuestProgressResponse> setDnf(
      @PathVariable Long questId, @PathVariable Long teamId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(questProgressService.setDnf(questId, teamId, auth));
  }
}
