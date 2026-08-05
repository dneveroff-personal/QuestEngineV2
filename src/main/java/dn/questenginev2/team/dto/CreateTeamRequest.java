package dn.questenginev2.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
    @NotBlank(message = "Название команды не может быть пустым")
        @Size(min = 1, max = 255, message = "Название команды должно быть от 1 до 255 символов")
        String name) {}
