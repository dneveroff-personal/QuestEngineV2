package dn.questenginev2.bonuspenalty.controller;

import dn.questenginev2.bonuspenalty.dto.CreateManualTimeAdjustmentRequest;
import dn.questenginev2.bonuspenalty.dto.ManualTimeAdjustmentResponse;
import dn.questenginev2.bonuspenalty.service.ManualTimeAdjustmentService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Ручная корректировка времени (ADR-0007, bonus-penalty.md) — один из
 * трёх источников итогового bonus/penalty наравне с эффектом кодов и
 * подсказок. Только Author конкретного Quest или ADMIN
 * (QuestService.validateQuestAuthor).
 */
@RestController
@RequiredArgsConstructor
@Tag(
    name = "Manual Time Adjustment",
    description = "Author/Admin-triggered time bonus or penalty for a team's QuestProgress")
public class ManualTimeAdjustmentController {

  private final ManualTimeAdjustmentService manualTimeAdjustmentService;

  @Operation(
      summary = "Create a manual time adjustment",
      description =
          "Author of the quest (or Admin) adds a bonus or penalty with a mandatory reason."
              + " No range limit on seconds (bonus-penalty.md — decided).")
  @PostMapping(Routes.QUEST_PROGRESS_BASE + Routes.ADJUSTMENTS)
  public ResponseEntity<ManualTimeAdjustmentResponse> create(
      @PathVariable Long questProgressId,
      @Valid @RequestBody CreateManualTimeAdjustmentRequest request,
      Authentication auth) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(manualTimeAdjustmentService.create(questProgressId, request, auth));
  }

  @Operation(
      summary = "List manual time adjustments for a QuestProgress",
      description = "Includes revoked entries (revokedAt/revokedByUserId set) for audit.")
  @GetMapping(Routes.QUEST_PROGRESS_BASE + Routes.ADJUSTMENTS)
  public ResponseEntity<List<ManualTimeAdjustmentResponse>> getByQuestProgress(
      @PathVariable Long questProgressId) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(manualTimeAdjustmentService.getByQuestProgressId(questProgressId));
  }

  @Operation(
      summary = "Revoke a manual time adjustment",
      description =
          "Allowed only before the quest is officially finished (bonus-penalty.md) — otherwise"
              + " results would change after being announced.")
  @PostMapping(Routes.ADJUSTMENTS_ROOT + Routes.REVOKE_ADJUSTMENT)
  public ResponseEntity<ManualTimeAdjustmentResponse> revoke(
      @PathVariable Long adjustmentId, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(manualTimeAdjustmentService.revoke(adjustmentId, auth));
  }
}
