package dn.questenginev2.level.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLevelRequest {

    @NotBlank
    private String title;

    private String content;
}
