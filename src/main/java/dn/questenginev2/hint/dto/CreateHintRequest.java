package dn.questenginev2.hint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateHintRequest(
    @NotNull Integer orderIndex, @NotNull Integer delaySeconds, @NotBlank String content) {}
