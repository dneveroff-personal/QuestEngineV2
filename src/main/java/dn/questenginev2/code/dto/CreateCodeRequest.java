package dn.questenginev2.code.dto;

import dn.questenginev2.code.entity.CodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCodeRequest {

    @NotBlank
    private String value;

    @NotNull
    private CodeType type;

    @NotNull
    private Integer points;
}
