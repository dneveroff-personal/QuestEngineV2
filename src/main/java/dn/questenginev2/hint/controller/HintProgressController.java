package dn.questenginev2.hint.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.hint.dto.HintProgressResponse;
import dn.questenginev2.hint.service.HintProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.QUEST_PROGRESS)
@Tag(
    name = "Hint Progress",
    description =
        "Hints visible to the team during active gameplay: REGULAR auto-revealed (ADR-0020),"
            + " BONUS/PENALTY require explicit take action (ADR-0021)")
public class HintProgressController {

  private final HintProgressService hintProgressService;

  @Operation(
      summary = "Get visible hints",
      description =
          "REGULAR hints already auto-revealed, plus BONUS/PENALTY hints either already taken"
              + " (full content) or available-but-not-taken (type only, no content/cost). Empty"
              + " list if the team has no active level.")
  @GetMapping(Routes.QUEST_PROGRESS_HINTS)
  public ResponseEntity<List<HintProgressResponse>> getVisibleHints(
      @PathVariable Long questId, @PathVariable Long teamId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(hintProgressService.getVisibleHints(questId, teamId, auth));
  }

  @Operation(
      summary = "Take a BONUS/PENALTY hint",
      description =
          "Any team member explicitly takes an available BONUS/PENALTY hint, revealing its"
              + " content and cost (ADR-0021). Idempotent — repeated calls return the same"
              + " already-revealed hint.")
  @PostMapping(Routes.QUEST_PROGRESS_HINT_TAKE)
  public ResponseEntity<HintProgressResponse> takeHint(
      @PathVariable Long questId,
      @PathVariable Long teamId,
      @PathVariable Long hintId,
      Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(hintProgressService.takeHint(questId, teamId, hintId, auth));
  }
}
