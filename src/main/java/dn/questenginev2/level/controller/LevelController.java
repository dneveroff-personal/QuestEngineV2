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

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.QUESTS)
@Tag(name = "Levels", description = "Quest level management endpoints")
public class LevelController {

    private final LevelService levelService;

    @Operation(summary = "Create level", description = "Create a new level for a quest")
    @PostMapping(Routes.QUEST_LEVELS)
    public ResponseEntity<LevelResponse> createLevel(
            @PathVariable Long questId,
            @Valid @RequestBody CreateLevelRequest request,
            Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(levelService.createLevel(questId, request, auth));
    }
}
