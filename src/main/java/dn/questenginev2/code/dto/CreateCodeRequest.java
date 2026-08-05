package dn.questenginev2.code.dto;

import dn.questenginev2.code.entity.CodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCodeRequest(
    @NotBlank String value, @NotNull CodeType type, @NotNull Integer points) {}
