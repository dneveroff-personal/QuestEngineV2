package dn.questenginev2.hint.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HintResponse {

    private Long id;
    private Long levelId;
    private Integer orderIndex;
    private Integer delaySeconds;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
