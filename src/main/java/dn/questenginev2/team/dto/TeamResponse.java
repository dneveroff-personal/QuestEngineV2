package dn.questenginev2.team.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class TeamResponse {

    private Long id;
    private String name;
    private String captainName;
    private Instant createdAt;

}
