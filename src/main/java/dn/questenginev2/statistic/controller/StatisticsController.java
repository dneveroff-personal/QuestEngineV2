package dn.questenginev2.statistic.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.statistic.dto.QuestStatisticsResponse;
import dn.questenginev2.statistic.service.StatisticsService;
import dn.questenginev2.statistic.sse.StatisticsSseHub;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.QUESTS)
@Tag(name = "Statistics", description = "Quest ranking table + SSE live updates (ADR-0014)")
public class StatisticsController {

  private final StatisticsService statisticsService;
  private final StatisticsSseHub statisticsSseHub;

  @Operation(
      summary = "Quest ranking table (snapshot)",
      description =
          "Live ranking while RUNNING; final ranking when FINISHED. See statistics-ranking.md.")
  @GetMapping(Routes.QUEST_ID + "/statistics")
  public ResponseEntity<QuestStatisticsResponse> getStatistics(@PathVariable Long questId) {
    return ResponseEntity.ok(statisticsService.getQuestStatistics(questId));
  }

  @Operation(
      summary = "Live ranking via SSE",
      description =
          "Initial event 'statistics' with full snapshot, then same event on each ranking change."
              + " Auth: Authorization Bearer or query access_token (for EventSource).")
  @GetMapping(
      value = Routes.QUEST_ID + "/statistics/stream",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamStatistics(@PathVariable Long questId) {
    // validates quest is RUNNING/FINISHED via initial snapshot inside hub
    return statisticsSseHub.subscribe(questId);
  }
}
