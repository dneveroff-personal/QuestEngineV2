package dn.questenginev2.quest.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.quest.dto.CreateQuestRequest;
import dn.questenginev2.quest.dto.QuestResponse;
import dn.questenginev2.quest.entity.QuestShortProjection;
import dn.questenginev2.quest.service.QuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.QUESTS)
@Tag(name = "Quests", description = "Quest management endpoints")
public class QuestController {

  private final QuestService questService;

  @Operation(summary = "Create quest", description = "Create a new quest")
  @PostMapping
  public ResponseEntity<QuestResponse> create(
      @Valid @RequestBody CreateQuestRequest request, Authentication auth) {
    return ResponseEntity.status(HttpStatus.CREATED).body(questService.createQuest(request, auth));
  }

  @Operation(summary = "Get quest by ID", description = "Retrieve quest details by ID")
  @GetMapping(Routes.QUEST_ID)
  public ResponseEntity<QuestResponse> getById(@PathVariable Long questId) {
    return ResponseEntity.status(HttpStatus.OK).body(questService.getQuestById(questId));
  }

  @Operation(
      summary = "Get all quests by author ID",
      description = "Retrieve all quests authored by the specified user")
  @GetMapping(Routes.QUEST_BY_AUTHOR)
  public ResponseEntity<List<QuestResponse>> getAllByAuthorId(@PathVariable Long authorId) {
    return ResponseEntity.status(HttpStatus.OK).body(questService.getAllByAuthorId(authorId));
  }

  @Operation(
      summary = "Get all upcoming quests",
      description = "Retrieve all upcoming quests in brief view")
  @GetMapping(Routes.QUEST_UPCOMING)
  public ResponseEntity<List<QuestShortProjection>> getAllUpcomingBrief() {
    return ResponseEntity.status(HttpStatus.OK).body(questService.getAllUpcomingBrief());
  }

  @Operation(summary = "Update quest", description = "Update existing quest")
  @PutMapping(Routes.QUEST_ID)
  public ResponseEntity<QuestResponse> updateQuest(
      @PathVariable Long questId,
      @Valid @RequestBody CreateQuestRequest request,
      Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(questService.updateQuest(questId, request, auth));
  }

  @Operation(
      summary = "Publish quest",
      description = "Transition quest from DRAFT to REGISTRATION, opening it for team sign-ups")
  @PostMapping(Routes.QUEST_PUBLISH)
  public ResponseEntity<QuestResponse> publish(@PathVariable Long questId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK).body(questService.publishQuest(questId, auth));
  }

  @Operation(
      summary = "Finish quest",
      description =
          "Transition quest from RUNNING to FINISHED. Teams that haven't finished receive DNF")
  @PostMapping(Routes.QUEST_FINISH)
  public ResponseEntity<QuestResponse> finish(@PathVariable Long questId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK).body(questService.finishQuest(questId, auth));
  }

  @Operation(summary = "Delete quest", description = "Delete quest by ID")
  @DeleteMapping(Routes.QUEST_ID)
  public ResponseEntity<Void> delete(@PathVariable Long questId, Authentication auth) {
    questService.delete(questId, auth);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
