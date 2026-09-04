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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.QUEST_PROGRESS)
@Tag(
    name = "Hint Progress",
    description = "Hints shown to the team during active gameplay (auto-reveal)")
public class HintProgressController {

  private final HintProgressService hintProgressService;

  @Operation(
      summary = "Get shown hints",
      description =
          "Hints already auto-revealed to the team on their current active level. Empty list if "
              + "the team has no active level.")
  @GetMapping(Routes.QUEST_PROGRESS_HINTS)
  public ResponseEntity<List<HintProgressResponse>> getShownHints(
      @PathVariable Long questId, @PathVariable Long teamId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(hintProgressService.getShownHints(questId, teamId, auth));
  }
}
