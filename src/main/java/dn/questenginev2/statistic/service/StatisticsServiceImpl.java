package dn.questenginev2.statistic.service;

import dn.questenginev2.bonuspenalty.service.BonusPenaltyService;
import dn.questenginev2.common.exceptions.ConflictException;
import dn.questenginev2.common.exceptions.ResourceNotFoundException;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.statistic.dto.QuestStatisticsResponse;
import dn.questenginev2.statistic.dto.StatisticsLevelCell;
import dn.questenginev2.statistic.dto.StatisticsLevelColumn;
import dn.questenginev2.statistic.dto.StatisticsTeamRow;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Ranking per {@code docs/01-domain/statistics-ranking.md}:
 *
 * <ul>
 *   <li>Runtime: more completed levels wins; tie → earlier last level completion.
 *   <li>Teams with 0 completed levels (still on L1) are omitted while Quest is RUNNING.
 *   <li>After Quest FINISHED: FINISHED by total time (wall + bonus/penalty); DNF at the bottom.
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

  private final QuestRepository questRepository;
  private final QuestProgressRepository questProgressRepository;
  private final LevelRepository levelRepository;
  private final LevelProgressRepository levelProgressRepository;
  private final BonusPenaltyService bonusPenaltyService;

  @Override
  public QuestStatisticsResponse getQuestStatistics(Long questId) {
    Quest quest =
        questRepository
            .findById(questId)
            .orElseThrow(() -> new ResourceNotFoundException("Квест не найден: " + questId));

    if (quest.getStatus() == QuestStatus.DRAFT || quest.getStatus() == QuestStatus.REGISTRATION) {
      throw new ConflictException(
          "Статистика доступна только после старта квеста (RUNNING/FINISHED)");
    }

    List<Level> levels = levelRepository.findByQuestIdOrderByOrderIndex(questId);
    List<StatisticsLevelColumn> columns =
        levels.stream()
            .map(
                l ->
                    StatisticsLevelColumn.builder()
                        .levelId(l.getId())
                        .orderIndex(l.getOrderIndex())
                        .title(l.getTitle())
                        .build())
            .toList();

    List<QuestProgress> progresses = questProgressRepository.findByQuestId(questId);
    List<LevelProgress> allLp = levelProgressRepository.findAllByQuestId(questId);
    Map<Long, List<LevelProgress>> lpByProgressId = new HashMap<>();
    for (LevelProgress lp : allLp) {
      lpByProgressId.computeIfAbsent(lp.getQuestProgress().getId(), k -> new ArrayList<>()).add(lp);
    }

    boolean questFinished = quest.getStatus() == QuestStatus.FINISHED;
    List<StatisticsTeamRow> rows = new ArrayList<>();

    for (QuestProgress qp : progresses) {
      List<LevelProgress> teamLp = lpByProgressId.getOrDefault(qp.getId(), List.of());
      List<LevelProgress> completed = teamLp.stream().filter(this::isCompleted).toList();

      // Rule 3: during live play, hide teams still on first level (0 completed).
      if (!questFinished && completed.isEmpty()) {
        continue;
      }

      int levelsCompleted = completed.size();
      Instant lastCompletedAt =
          completed.stream()
              .map(LevelProgress::getCompletedAt)
              .filter(Objects::nonNull)
              .max(Instant::compareTo)
              .orElse(null);
      Integer lastOrder =
          completed.stream()
              .filter(lp -> lp.getCompletedAt() != null)
              .max(Comparator.comparing(LevelProgress::getCompletedAt))
              .map(lp -> lp.getLevel().getOrderIndex())
              .orElse(null);

      long bonusPenalty = bonusPenaltyService.getTotalAdjustmentSeconds(qp);
      Long totalTime = null;
      if (qp.getStatus() == QuestProgressStatus.FINISHED
          && qp.getFinishedAt() != null
          && qp.getQuestStartedAt() != null) {
        long wall = Duration.between(qp.getQuestStartedAt(), qp.getFinishedAt()).getSeconds();
        totalTime = wall + bonusPenalty;
      }

      List<StatisticsLevelCell> cells =
          teamLp.stream()
              .sorted(Comparator.comparing(lp -> lp.getLevel().getOrderIndex()))
              .map(
                  lp ->
                      StatisticsLevelCell.builder()
                          .orderIndex(lp.getLevel().getOrderIndex())
                          .levelId(lp.getLevel().getId())
                          .status(lp.getStatus())
                          .openedAt(lp.getOpenedAt())
                          .completedAt(lp.getCompletedAt())
                          .build())
              .toList();

      rows.add(
          StatisticsTeamRow.builder()
              .teamId(qp.getTeam().getId())
              .teamName(qp.getTeam().getName())
              .progressStatus(qp.getStatus())
              .levelsCompleted(levelsCompleted)
              .lastCompletedLevelOrderIndex(lastOrder)
              .lastLevelCompletedAt(lastCompletedAt)
              .levelCells(cells)
              .bonusPenaltySeconds(bonusPenalty)
              .totalTimeSeconds(totalTime)
              .build());
    }

    sortAndAssignRanks(rows, questFinished);

    return QuestStatisticsResponse.builder()
        .questId(quest.getId())
        .questTitle(quest.getTitle())
        .questStatus(quest.getStatus())
        .levels(columns)
        .rows(rows)
        .build();
  }

  private boolean isCompleted(LevelProgress lp) {
    return lp.getStatus() == LevelProgressStatus.COMPLETED
        || lp.getStatus() == LevelProgressStatus.AUTO_TRANSITIONED;
  }

  private void sortAndAssignRanks(List<StatisticsTeamRow> rows, boolean questFinished) {
    if (questFinished) {
      rows.sort(finalRankingComparator());
    } else {
      rows.sort(runtimeRankingComparator());
    }
    int rank = 1;
    for (StatisticsTeamRow row : rows) {
      row.setRank(rank++);
    }
  }

  /** Rule 1 + Rule 2. */
  private Comparator<StatisticsTeamRow> runtimeRankingComparator() {
    return Comparator.comparingInt(StatisticsTeamRow::getLevelsCompleted)
        .reversed()
        .thenComparing(
            StatisticsTeamRow::getLastLevelCompletedAt,
            Comparator.nullsLast(Comparator.naturalOrder()));
  }

  /** FINISHED by totalTime ASC; DNF after all finishers. */
  private Comparator<StatisticsTeamRow> finalRankingComparator() {
    return (a, b) -> {
      boolean aFin = a.getProgressStatus() == QuestProgressStatus.FINISHED;
      boolean bFin = b.getProgressStatus() == QuestProgressStatus.FINISHED;
      if (aFin && !bFin) {
        return -1;
      }
      if (!aFin && bFin) {
        return 1;
      }
      if (aFin && bFin) {
        long ta = a.getTotalTimeSeconds() != null ? a.getTotalTimeSeconds() : Long.MAX_VALUE;
        long tb = b.getTotalTimeSeconds() != null ? b.getTotalTimeSeconds() : Long.MAX_VALUE;
        return Long.compare(ta, tb);
      }
      // both non-finished (e.g. DNF): more levels then earlier last completion
      return runtimeRankingComparator().compare(a, b);
    };
  }
}
