package dn.questenginev2.statistic.sse;

import dn.questenginev2.statistic.dto.QuestStatisticsResponse;
import dn.questenginev2.statistic.service.StatisticsService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-memory SSE registry per quest (ADR-0014). Suitable for single-instance deploy; multi-instance
 * would need a shared pub/sub later.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatisticsSseHub {

  private static final long TIMEOUT_MS = 30 * 60 * 1000L;

  private final StatisticsService statisticsService;
  private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByQuest =
      new ConcurrentHashMap<>();

  public SseEmitter subscribe(Long questId) {
    SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
    emittersByQuest.computeIfAbsent(questId, id -> new CopyOnWriteArrayList<>()).add(emitter);

    emitter.onCompletion(() -> remove(questId, emitter));
    emitter.onTimeout(() -> remove(questId, emitter));
    emitter.onError(e -> remove(questId, emitter));

    try {
      QuestStatisticsResponse snapshot = statisticsService.getQuestStatistics(questId);
      emitter.send(SseEmitter.event().name("statistics").data(snapshot));
    } catch (Exception e) {
      log.warn("SSE initial snapshot failed for quest {}: {}", questId, e.getMessage());
      emitter.completeWithError(e);
      remove(questId, emitter);
    }

    return emitter;
  }

  public void publish(Long questId) {
    List<SseEmitter> emitters = emittersByQuest.get(questId);
    if (emitters == null || emitters.isEmpty()) {
      return;
    }

    QuestStatisticsResponse snapshot;
    try {
      snapshot = statisticsService.getQuestStatistics(questId);
    } catch (Exception e) {
      log.warn("SSE rebuild snapshot failed for quest {}: {}", questId, e.getMessage());
      return;
    }

    for (SseEmitter emitter : List.copyOf(emitters)) {
      try {
        emitter.send(SseEmitter.event().name("statistics").data(snapshot));
      } catch (IOException ex) {
        remove(questId, emitter);
        try {
          emitter.complete();
        } catch (Exception ignored) {
          // already dead
        }
      }
    }
  }

  private void remove(Long questId, SseEmitter emitter) {
    CopyOnWriteArrayList<SseEmitter> list = emittersByQuest.get(questId);
    if (list != null) {
      list.remove(emitter);
      if (list.isEmpty()) {
        emittersByQuest.remove(questId, list);
      }
    }
  }
}
