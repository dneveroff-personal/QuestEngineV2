package dn.questenginev2.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTeamRequest {

    @NotBlank
    @Size(min = 1, max = 255)
    private String name;

}
