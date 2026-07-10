package dn.questenginev2.level.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LevelResponse {

    private Long id;
    private Long questId;
    private String title;
    private Integer orderIndex;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
