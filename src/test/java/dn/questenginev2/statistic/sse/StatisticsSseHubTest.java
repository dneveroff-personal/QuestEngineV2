package dn.questenginev2.statistic.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dn.questenginev2.common.exceptions.ConflictException;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.statistic.dto.QuestStatisticsResponse;
import dn.questenginev2.statistic.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class StatisticsSseHubTest {

  @Mock private StatisticsService statisticsService;

  @InjectMocks private StatisticsSseHub statisticsSseHub;

  @Test
  void subscribe_returnsEmitterAndLoadsInitialSnapshot() {
    QuestStatisticsResponse snapshot =
        QuestStatisticsResponse.builder()
            .questId(1L)
            .questTitle("Q")
            .questStatus(QuestStatus.RUNNING)
            .levels(java.util.List.of())
            .rows(java.util.List.of())
            .build();
    when(statisticsService.getQuestStatistics(1L)).thenReturn(snapshot);

    SseEmitter emitter = statisticsSseHub.subscribe(1L);

    assertThat(emitter).isNotNull();
    verify(statisticsService, times(1)).getQuestStatistics(1L);
  }

  @Test
  void subscribe_completesWithError_whenSnapshotFails() {
    when(statisticsService.getQuestStatistics(2L))
        .thenThrow(new ConflictException("Статистика недоступна"));

    SseEmitter emitter = statisticsSseHub.subscribe(2L);

    assertThat(emitter).isNotNull();
    verify(statisticsService).getQuestStatistics(2L);
  }

  @Test
  void publish_doesNothing_whenNoSubscribers() {
    statisticsSseHub.publish(99L);

    verify(statisticsService, never()).getQuestStatistics(99L);
  }

  @Test
  void publish_rebuildsSnapshot_forActiveSubscribers() {
    QuestStatisticsResponse snapshot =
        QuestStatisticsResponse.builder()
            .questId(5L)
            .questTitle("Live")
            .questStatus(QuestStatus.RUNNING)
            .levels(java.util.List.of())
            .rows(java.util.List.of())
            .build();
    when(statisticsService.getQuestStatistics(5L)).thenReturn(snapshot);

    statisticsSseHub.subscribe(5L);
    statisticsSseHub.publish(5L);

    // initial subscribe + publish
    verify(statisticsService, times(2)).getQuestStatistics(5L);
  }
}
