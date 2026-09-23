package dn.questenginev2.statistic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.statistic.dto.QuestStatisticsResponse;
import dn.questenginev2.statistic.dto.StatisticsTeamRow;
import dn.questenginev2.team.entity.Team;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

  @Mock private QuestRepository questRepository;
  @Mock private QuestProgressRepository questProgressRepository;
  @Mock private LevelRepository levelRepository;
  @Mock private LevelProgressRepository levelProgressRepository;
  @Mock private BonusPenaltyService bonusPenaltyService;

  @InjectMocks private StatisticsServiceImpl statisticsService;

  private Quest runningQuest;
  private Quest finishedQuest;
  private Level level1;
  private Level level2;
  private Team teamA;
  private Team teamB;
  private Team teamC;

  private final Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
  private final Instant t1 = Instant.parse("2026-01-01T10:10:00Z");
  private final Instant t2 = Instant.parse("2026-01-01T10:20:00Z");
  private final Instant t3 = Instant.parse("2026-01-01T10:30:00Z");

  @BeforeEach
  void setUp() {
    runningQuest =
        Quest.builder()
            .id(1L)
            .title("Live Quest")
            .type(QuestType.TEAM)
            .status(QuestStatus.RUNNING)
            .createdAt(t0)
            .build();
    finishedQuest =
        Quest.builder()
            .id(2L)
            .title("Done Quest")
            .type(QuestType.TEAM)
            .status(QuestStatus.FINISHED)
            .createdAt(t0)
            .build();
    level1 = Level.builder().id(10L).quest(runningQuest).title("L1").orderIndex(1).build();
    level2 = Level.builder().id(11L).quest(runningQuest).title("L2").orderIndex(2).build();
    teamA = Team.builder().id(100L).name("Alpha").build();
    teamB = Team.builder().id(101L).name("Beta").build();
    teamC = Team.builder().id(102L).name("Gamma").build();
  }

  @Test
  void getQuestStatistics_throwsNotFound_whenQuestMissing() {
    when(questRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> statisticsService.getQuestStatistics(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Квест не найден");
  }

  @Test
  void getQuestStatistics_throwsConflict_whenDraft() {
    Quest draft =
        Quest.builder()
            .id(3L)
            .title("Draft")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .createdAt(t0)
            .build();
    when(questRepository.findById(3L)).thenReturn(Optional.of(draft));

    assertThatThrownBy(() -> statisticsService.getQuestStatistics(3L))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("RUNNING/FINISHED");
  }

  @Test
  void getQuestStatistics_throwsConflict_whenRegistration() {
    Quest reg =
        Quest.builder()
            .id(4L)
            .title("Reg")
            .type(QuestType.TEAM)
            .status(QuestStatus.REGISTRATION)
            .createdAt(t0)
            .build();
    when(questRepository.findById(4L)).thenReturn(Optional.of(reg));

    assertThatThrownBy(() -> statisticsService.getQuestStatistics(4L))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("RUNNING/FINISHED");
  }

  @Test
  void runtimeRanking_hidesTeamsWithZeroCompletedLevels() {
    QuestProgress qpA =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(teamA)
            .status(QuestProgressStatus.RUNNING)
            .questStartedAt(t0)
            .build();
    QuestProgress qpB =
        QuestProgress.builder()
            .id(2L)
            .quest(runningQuest)
            .team(teamB)
            .status(QuestProgressStatus.RUNNING)
            .questStartedAt(t0)
            .build();

    // A completed L1; B still on L1 (ACTIVE only)
    LevelProgress aL1 =
        LevelProgress.builder()
            .id(20L)
            .questProgress(qpA)
            .level(level1)
            .status(LevelProgressStatus.COMPLETED)
            .openedAt(t0)
            .completedAt(t1)
            .build();
    LevelProgress bL1 =
        LevelProgress.builder()
            .id(21L)
            .questProgress(qpB)
            .level(level1)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(t0)
            .build();

    when(questRepository.findById(1L)).thenReturn(Optional.of(runningQuest));
    when(levelRepository.findByQuestIdOrderByOrderIndex(1L)).thenReturn(List.of(level1, level2));
    when(questProgressRepository.findByQuestId(1L)).thenReturn(List.of(qpA, qpB));
    when(levelProgressRepository.findAllByQuestId(1L)).thenReturn(List.of(aL1, bL1));
    when(bonusPenaltyService.getTotalAdjustmentSeconds(qpA)).thenReturn(0L);

    QuestStatisticsResponse response = statisticsService.getQuestStatistics(1L);

    assertThat(response.getQuestStatus()).isEqualTo(QuestStatus.RUNNING);
    assertThat(response.getLevels()).hasSize(2);
    assertThat(response.getRows()).hasSize(1);
    assertThat(response.getRows().get(0).getTeamName()).isEqualTo("Alpha");
    assertThat(response.getRows().get(0).getRank()).isEqualTo(1);
    assertThat(response.getRows().get(0).getLevelsCompleted()).isEqualTo(1);
  }

  @Test
  void runtimeRanking_moreCompletedLevelsWins_thenEarlierLastCompletion() {
    QuestProgress qpA =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(teamA)
            .status(QuestProgressStatus.RUNNING)
            .questStartedAt(t0)
            .build();
    QuestProgress qpB =
        QuestProgress.builder()
            .id(2L)
            .quest(runningQuest)
            .team(teamB)
            .status(QuestProgressStatus.RUNNING)
            .questStartedAt(t0)
            .build();
    QuestProgress qpC =
        QuestProgress.builder()
            .id(3L)
            .quest(runningQuest)
            .team(teamC)
            .status(QuestProgressStatus.RUNNING)
            .questStartedAt(t0)
            .build();

    // A: 2 levels, last at t2
    LevelProgress aL1 =
        completedLp(30L, qpA, level1, t0, t1);
    LevelProgress aL2 =
        completedLp(31L, qpA, level2, t1, t2);
    // B: 2 levels, last at t3 (later → worse)
    LevelProgress bL1 =
        completedLp(32L, qpB, level1, t0, t1);
    LevelProgress bL2 =
        completedLp(33L, qpB, level2, t1, t3);
    // C: 1 level only
    LevelProgress cL1 =
        completedLp(34L, qpC, level1, t0, t1);

    when(questRepository.findById(1L)).thenReturn(Optional.of(runningQuest));
    when(levelRepository.findByQuestIdOrderByOrderIndex(1L)).thenReturn(List.of(level1, level2));
    when(questProgressRepository.findByQuestId(1L)).thenReturn(List.of(qpA, qpB, qpC));
    when(levelProgressRepository.findAllByQuestId(1L))
        .thenReturn(List.of(aL1, aL2, bL1, bL2, cL1));
    when(bonusPenaltyService.getTotalAdjustmentSeconds(qpA)).thenReturn(0L);
    when(bonusPenaltyService.getTotalAdjustmentSeconds(qpB)).thenReturn(0L);
    when(bonusPenaltyService.getTotalAdjustmentSeconds(qpC)).thenReturn(0L);

    QuestStatisticsResponse response = statisticsService.getQuestStatistics(1L);

    assertThat(response.getRows()).hasSize(3);
    assertThat(response.getRows().get(0).getTeamName()).isEqualTo("Alpha"); // 2 levels, earlier
    assertThat(response.getRows().get(0).getRank()).isEqualTo(1);
    assertThat(response.getRows().get(1).getTeamName()).isEqualTo("Beta"); // 2 levels, later
    assertThat(response.getRows().get(1).getRank()).isEqualTo(2);
    assertThat(response.getRows().get(2).getTeamName()).isEqualTo("Gamma"); // 1 level
    assertThat(response.getRows().get(2).getRank()).isEqualTo(3);
  }

  @Test
  void finalRanking_finishedByTotalTime_dnfAtBottom() {
    Level l1 = Level.builder().id(10L).quest(finishedQuest).title("L1").orderIndex(1).build();
    Level l2 = Level.builder().id(11L).quest(finishedQuest).title("L2").orderIndex(2).build();

    QuestProgress qpA =
        QuestProgress.builder()
            .id(1L)
            .quest(finishedQuest)
            .team(teamA)
            .status(QuestProgressStatus.FINISHED)
            .questStartedAt(t0)
            .finishedAt(t2) // wall 20 min = 1200s
            .build();
    QuestProgress qpB =
        QuestProgress.builder()
            .id(2L)
            .quest(finishedQuest)
            .team(teamB)
            .status(QuestProgressStatus.FINISHED)
            .questStartedAt(t0)
            .finishedAt(t3) // wall 30 min = 1800s
            .build();
    QuestProgress qpC =
        QuestProgress.builder()
            .id(3L)
            .quest(finishedQuest)
            .team(teamC)
            .status(QuestProgressStatus.DNF)
            .questStartedAt(t0)
            .build();

    LevelProgress aL1 = completedLp(40L, qpA, l1, t0, t1);
    LevelProgress aL2 = completedLp(41L, qpA, l2, t1, t2);
    LevelProgress bL1 = completedLp(42L, qpB, l1, t0, t1);
    LevelProgress bL2 = completedLp(43L, qpB, l2, t1, t3);
    LevelProgress cL1 = completedLp(44L, qpC, l1, t0, t1);

    when(questRepository.findById(2L)).thenReturn(Optional.of(finishedQuest));
    when(levelRepository.findByQuestIdOrderByOrderIndex(2L)).thenReturn(List.of(l1, l2));
    when(questProgressRepository.findByQuestId(2L)).thenReturn(List.of(qpA, qpB, qpC));
    when(levelProgressRepository.findAllByQuestId(2L))
        .thenReturn(List.of(aL1, aL2, bL1, bL2, cL1));
    when(bonusPenaltyService.getTotalAdjustmentSeconds(qpA)).thenReturn(-60L); // total 1140
    when(bonusPenaltyService.getTotalAdjustmentSeconds(qpB)).thenReturn(0L); // total 1800
    when(bonusPenaltyService.getTotalAdjustmentSeconds(qpC)).thenReturn(0L);

    QuestStatisticsResponse response = statisticsService.getQuestStatistics(2L);

    assertThat(response.getQuestStatus()).isEqualTo(QuestStatus.FINISHED);
    assertThat(response.getRows()).hasSize(3);

    StatisticsTeamRow first = response.getRows().get(0);
    assertThat(first.getTeamName()).isEqualTo("Alpha");
    assertThat(first.getRank()).isEqualTo(1);
    assertThat(first.getTotalTimeSeconds()).isEqualTo(1140L);

    StatisticsTeamRow second = response.getRows().get(1);
    assertThat(second.getTeamName()).isEqualTo("Beta");
    assertThat(second.getRank()).isEqualTo(2);
    assertThat(second.getTotalTimeSeconds()).isEqualTo(1800L);

    StatisticsTeamRow third = response.getRows().get(2);
    assertThat(third.getTeamName()).isEqualTo("Gamma");
    assertThat(third.getProgressStatus()).isEqualTo(QuestProgressStatus.DNF);
    assertThat(third.getRank()).isEqualTo(3);
  }

  @Test
  void runtimeRanking_countsAutoTransitionedAsCompleted() {
    QuestProgress qpA =
        QuestProgress.builder()
            .id(1L)
            .quest(runningQuest)
            .team(teamA)
            .status(QuestProgressStatus.RUNNING)
            .questStartedAt(t0)
            .build();
    LevelProgress auto =
        LevelProgress.builder()
            .id(50L)
            .questProgress(qpA)
            .level(level1)
            .status(LevelProgressStatus.AUTO_TRANSITIONED)
            .openedAt(t0)
            .completedAt(t1)
            .build();

    when(questRepository.findById(1L)).thenReturn(Optional.of(runningQuest));
    when(levelRepository.findByQuestIdOrderByOrderIndex(1L)).thenReturn(List.of(level1));
    when(questProgressRepository.findByQuestId(1L)).thenReturn(List.of(qpA));
    when(levelProgressRepository.findAllByQuestId(1L)).thenReturn(List.of(auto));
    when(bonusPenaltyService.getTotalAdjustmentSeconds(qpA)).thenReturn(0L);

    QuestStatisticsResponse response = statisticsService.getQuestStatistics(1L);

    assertThat(response.getRows()).hasSize(1);
    assertThat(response.getRows().get(0).getLevelsCompleted()).isEqualTo(1);
  }

  private static LevelProgress completedLp(
      Long id, QuestProgress qp, Level level, Instant opened, Instant completed) {
    return LevelProgress.builder()
        .id(id)
        .questProgress(qp)
        .level(level)
        .status(LevelProgressStatus.COMPLETED)
        .openedAt(opened)
        .completedAt(completed)
        .build();
  }
}
