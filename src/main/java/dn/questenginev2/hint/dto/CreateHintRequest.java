package dn.questenginev2.hint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateHintRequest {

  @NotNull private Integer orderIndex;

  @NotNull private Integer delaySeconds;

  @NotBlank private String content;
}
