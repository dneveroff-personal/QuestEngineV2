package dn.questenginev2.level.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLevelRequest(@NotBlank String title, String content, Integer timeoutSeconds) {}
