package dn.questenginev2.code.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.code.dto.CreateCodeRequest;
import dn.questenginev2.code.dto.CodeResponse;
import dn.questenginev2.code.service.CodeService;
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
@Tag(name = "Codes", description = "Level code management endpoints")
public class CodeController {

    private final CodeService codeService;

    @Operation(summary = "Create code", description = "Create a new code for a level")
    @PostMapping(Routes.QUESTS + Routes.LEVEL_CODES)
    public ResponseEntity<CodeResponse> createCode(
            @PathVariable Long questId,
            @PathVariable Long levelId,
            @Valid @RequestBody CreateCodeRequest request,
            Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(codeService.createCode(levelId, request, auth));
    }

    @Operation(summary = "Get codes by level", description = "Get all codes for a level")
    @GetMapping(Routes.QUESTS + Routes.LEVEL_CODES)
    public ResponseEntity<List<CodeResponse>> getCodesByLevel(
            @PathVariable Long questId,
            @PathVariable Long levelId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(codeService.getCodesByLevelId(levelId));
    }

    @Operation(summary = "Get code by ID", description = "Get code details by ID")
    @GetMapping(Routes.CODES + Routes.CODE_ID)
    public ResponseEntity<CodeResponse> getCodeById(@PathVariable Long codeId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(codeService.getCodeById(codeId));
    }

    @Operation(summary = "Update code", description = "Update existing code")
    @PutMapping(Routes.CODES + Routes.CODE_ID)
    public ResponseEntity<CodeResponse> updateCode(
            @PathVariable Long codeId,
            @Valid @RequestBody CreateCodeRequest request,
            Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(codeService.updateCode(codeId, request, auth));
    }

    @Operation(summary = "Delete code", description = "Delete code by ID")
    @DeleteMapping(Routes.CODES + Routes.CODE_ID)
    public ResponseEntity<Void> deleteCode(@PathVariable Long codeId, Authentication auth) {
        codeService.deleteCode(codeId, auth);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }
}
