package dn.questenginev2.level.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLevelRequest {

    @NotBlank
    private String title;

    @NotNull
    private Integer orderIndex;

    private String content;
}
