package dn.questenginev2.code.controller;

import dn.questenginev2.code.dto.CodeSubmissionResponse;
import dn.questenginev2.code.dto.SubmitCodeRequest;
import dn.questenginev2.code.service.CodeSubmissionService;
import dn.questenginev2.common.constants.Routes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.QUEST_PROGRESS)
@Tag(name = "Code Submission", description = "Code entry during active gameplay")
public class CodeSubmissionController {

  private final CodeSubmissionService codeSubmissionService;

  @Operation(
      summary = "Submit code",
      description =
          "Any team member submits a code on the team's currently active level. "
              + "Not rate-limited by design (speed is part of the gameplay, see ADR-0016).")
  @PostMapping(Routes.QUEST_PROGRESS_CODES)
  public ResponseEntity<CodeSubmissionResponse> submitCode(
      @PathVariable Long questId,
      @PathVariable Long teamId,
      @Valid @RequestBody SubmitCodeRequest request,
      Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(codeSubmissionService.submitCode(questId, teamId, request, auth));
  }
}
