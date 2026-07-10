package dn.questenginev2.hint.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.hint.dto.CreateHintRequest;
import dn.questenginev2.hint.dto.HintResponse;
import dn.questenginev2.hint.service.HintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Hints", description = "Level hint management endpoints")
public class HintController {

    private final HintService hintService;

    @Operation(summary = "Create hint", description = "Create a new hint for a level")
    @PostMapping(Routes.QUESTS + Routes.LEVEL_HINTS)
    public ResponseEntity<HintResponse> createHint(
            @PathVariable Long questId,
            @PathVariable Long levelId,
            @Valid @RequestBody CreateHintRequest request,
            Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(hintService.createHint(levelId, request, auth));
    }

    @Operation(summary = "Get hints by level", description = "Get all hints for a level")
    @GetMapping(Routes.QUESTS + Routes.LEVEL_HINTS)
    public ResponseEntity<List<HintResponse>> getHintsByLevel(
            @PathVariable Long questId,
            @PathVariable Long levelId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(hintService.getHintsByLevelId(levelId));
    }

    @Operation(summary = "Get hint by ID", description = "Get hint details by ID")
    @GetMapping(Routes.HINTS + Routes.HINT_ID)
    public ResponseEntity<HintResponse> getHintById(@PathVariable Long hintId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(hintService.getHintById(hintId));
    }

    @Operation(summary = "Update hint", description = "Update existing hint")
    @PutMapping(Routes.HINTS + Routes.HINT_ID)
    public ResponseEntity<HintResponse> updateHint(
            @PathVariable Long hintId,
            @Valid @RequestBody CreateHintRequest request,
            Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(hintService.updateHint(hintId, request, auth));
    }

    @Operation(summary = "Delete hint", description = "Delete hint by ID")
    @DeleteMapping(Routes.HINTS + Routes.HINT_ID)
    public ResponseEntity<Void> deleteHint(@PathVariable Long hintId) {
        hintService.deleteHint(hintId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }
}
