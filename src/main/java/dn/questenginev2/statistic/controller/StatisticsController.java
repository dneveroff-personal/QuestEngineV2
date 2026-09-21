package dn.questenginev2.statistic.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.statistic.dto.QuestStatisticsResponse;
import dn.questenginev2.statistic.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.QUESTS)
@Tag(name = "Statistics", description = "Quest ranking table (snapshot; SSE is backlog #19)")
public class StatisticsController {

  private final StatisticsService statisticsService;

  @Operation(
      summary = "Quest ranking table",
      description =
          "Live ranking while RUNNING (teams with ≥1 completed level). Final ranking when"
              + " FINISHED (by total time; DNF at bottom). See statistics-ranking.md.")
  @GetMapping(Routes.QUEST_ID + "/statistics")
  public ResponseEntity<QuestStatisticsResponse> getStatistics(@PathVariable Long questId) {
    return ResponseEntity.ok(statisticsService.getQuestStatistics(questId));
  }
}
