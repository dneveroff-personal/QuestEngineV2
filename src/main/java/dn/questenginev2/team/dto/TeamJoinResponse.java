package dn.questenginev2.team.dto;

import dn.questenginev2.team.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class TeamJoinResponse {

    private Long requestId;
    private String userName;
    private RequestStatus status;
    private Instant createdAt;

}
