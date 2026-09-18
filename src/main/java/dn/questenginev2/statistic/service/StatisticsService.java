package dn.questenginev2.statistic.service;

import dn.questenginev2.statistic.dto.QuestStatisticsResponse;

public interface StatisticsService {

  /** Ranking table snapshot for a quest (live or final). Requires authenticated user. */
  QuestStatisticsResponse getQuestStatistics(Long questId);
}
