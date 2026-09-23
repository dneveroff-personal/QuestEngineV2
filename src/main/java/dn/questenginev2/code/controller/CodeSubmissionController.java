package dn.questenginev2.code.controller;

import dn.questenginev2.code.dto.CodeSubmissionAttemptResponse;
import dn.questenginev2.code.dto.CodeSubmissionResponse;
import dn.questenginev2.code.dto.SubmitCodeRequest;
import dn.questenginev2.code.service.CodeSubmissionService;
import dn.questenginev2.common.constants.Routes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "Code Submission", description = "Code entry during active gameplay and attempt audit")
public class CodeSubmissionController {

  private final CodeSubmissionService codeSubmissionService;

  @Operation(
      summary = "Submit code",
      description =
          "Any team member submits a code on the team's currently active level. "
              + "Not rate-limited by design (speed is part of the gameplay, see ADR-0016).")
  @PostMapping(Routes.QUEST_PROGRESS + Routes.QUEST_PROGRESS_CODES)
  public ResponseEntity<CodeSubmissionResponse> submitCode(
      @PathVariable Long questId,
      @PathVariable Long teamId,
      @Valid @RequestBody SubmitCodeRequest request,
      Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(codeSubmissionService.submitCode(questId, teamId, request, auth));
  }

  @Operation(
      summary = "List team attempts on active level",
      description =
          "Own attempts of the team on the currently ACTIVE LevelProgress "
              + "(statistics-ranking.md / code-submission.md). Empty list if no active level. "
              + "Allowed for team members and ADMIN.")
  @GetMapping(Routes.QUEST_PROGRESS + Routes.QUEST_PROGRESS_CODES)
  public ResponseEntity<List<CodeSubmissionAttemptResponse>> listTeamAttempts(
      @PathVariable Long questId, @PathVariable Long teamId, Authentication auth) {
    return ResponseEntity.ok(
        codeSubmissionService.listAttemptsForTeamActiveLevel(questId, teamId, auth));
  }

  @Operation(
      summary = "List all code attempts for quest (author)",
      description =
          "Full attempt audit across all teams and levels. "
              + "Allowed for quest author and ADMIN only.")
  @GetMapping(Routes.QUESTS + Routes.QUEST_CODE_SUBMISSIONS)
  public ResponseEntity<List<CodeSubmissionAttemptResponse>> listAuthorAttempts(
      @PathVariable Long questId, Authentication auth) {
    return ResponseEntity.ok(codeSubmissionService.listAttemptsForQuestAuthor(questId, auth));
  }
}
