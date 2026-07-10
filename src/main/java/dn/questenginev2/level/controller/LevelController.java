package dn.questenginev2.level.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.level.dto.CreateLevelRequest;
import dn.questenginev2.level.dto.LevelResponse;
import dn.questenginev2.level.service.LevelService;
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
@Tag(name = "Levels", description = "Quest level management endpoints")
public class LevelController {

    private final LevelService levelService;

    @Operation(summary = "Create level", description = "Create a new level for a quest")
    @PostMapping(Routes.QUESTS + Routes.QUEST_LEVELS)
    public ResponseEntity<LevelResponse> createLevel(
            @PathVariable Long questId,
            @Valid @RequestBody CreateLevelRequest request,
            Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(levelService.createLevel(questId, request, auth));
    }

    @Operation(summary = "Get levels by quest", description = "Get all levels for a quest")
    @GetMapping(Routes.QUESTS + Routes.QUEST_LEVELS)
    public ResponseEntity<List<LevelResponse>> getLevelsByQuest(@PathVariable Long questId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(levelService.getLevelsByQuestId(questId));
    }

    @Operation(summary = "Get level by ID", description = "Get level details by ID")
    @GetMapping(Routes.LEVELS + Routes.LEVEL_ID)
    public ResponseEntity<LevelResponse> getLevelById(@PathVariable Long levelId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(levelService.getLevelById(levelId));
    }

    @Operation(summary = "Update level", description = "Update existing level")
    @PutMapping(Routes.LEVELS + Routes.LEVEL_ID)
    public ResponseEntity<LevelResponse> updateLevel(
            @PathVariable Long levelId,
            @Valid @RequestBody CreateLevelRequest request,
            Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(levelService.updateLevel(levelId, request, auth));
    }

    @Operation(summary = "Delete level", description = "Delete level by ID")
    @DeleteMapping(Routes.LEVELS + Routes.LEVEL_ID)
    public ResponseEntity<Void> deleteLevel(@PathVariable Long levelId) {
        levelService.deleteLevel(levelId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }
}
