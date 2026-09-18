package dn.questenginev2.statistic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsLevelColumn {

  private Long levelId;
  private Integer orderIndex;
  private String title;
}
