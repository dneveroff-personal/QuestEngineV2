package dn.questenginev2.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.gameplay.event.GameplayEventPublisher;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.quest.dto.QuestProgressResponse;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.service.QuestProgressService;
import java.time.Clock;
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
class LevelAutoTransitionSchedulerTest {

  @Mock private LevelProgressRepository levelProgressRepository;
  @Mock private QuestProgressService questProgressService;
  @Mock private Clock clock;
  @Mock private GameplayEventPublisher gameplayEventPublisher;

  @InjectMocks private LevelAutoTransitionScheduler levelAutoTransitionScheduler;

  private final Instant fixedNow = Instant.parse("2026-08-24T21:00:00Z");

  private Level level;
  private Quest quest;
  private QuestProgress questProgress;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(fixedNow);
    level = Level.builder().id(1000L).title("L1").orderIndex(1).build();
    quest = Quest.builder().id(10L).title("Q1").description("desc").build();
    questProgress = QuestProgress.builder().id(500L).quest(quest).build();
  }

  private LevelProgress levelProgress(Long id) {
    return LevelProgress.builder()
        .id(id)
        .level(level)
        .questProgress(questProgress)
        .status(LevelProgressStatus.ACTIVE)
        .build();
  }

  @Test
  void autoTransitionDueLevels_doesNothing_whenNoCandidates() {
    when(levelProgressRepository.findByStatusAndAutoTransitionAtLessThanEqual(
            LevelProgressStatus.ACTIVE, fixedNow))
        .thenReturn(List.of());

    levelAutoTransitionScheduler.autoTransitionDueLevels();

    verify(levelProgressRepository, never()).tryAutoTransition(any(), any());
  }

  @Test
  void autoTransitionDueLevels_advancesLevel_whenTransitionWon() {
    LevelProgress candidate = levelProgress(1L);
    when(levelProgressRepository.findByStatusAndAutoTransitionAtLessThanEqual(
            LevelProgressStatus.ACTIVE, fixedNow))
        .thenReturn(List.of(candidate));
    when(levelProgressRepository.tryAutoTransition(1L, fixedNow)).thenReturn(1);
    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(candidate));
    when(questProgressService.advanceAfterLevelCompleted(any()))
        .thenReturn(QuestProgressResponse.builder().build());

    levelAutoTransitionScheduler.autoTransitionDueLevels();

    verify(questProgressService).advanceAfterLevelCompleted(candidate);
  }

  @Test
  void autoTransitionDueLevels_skipsLevel_whenTransitionLost() {
    // Сценарий 5: CodeSubmission параллельно уже завершил этот уровень кодом.
    LevelProgress candidate = levelProgress(1L);
    when(levelProgressRepository.findByStatusAndAutoTransitionAtLessThanEqual(
            LevelProgressStatus.ACTIVE, fixedNow))
        .thenReturn(List.of(candidate));
    when(levelProgressRepository.tryAutoTransition(1L, fixedNow)).thenReturn(0);

    levelAutoTransitionScheduler.autoTransitionDueLevels();

    verify(questProgressService, never()).advanceAfterLevelCompleted(any());
    verify(levelProgressRepository, never()).findById(any());
  }

  @Test
  void autoTransitionDueLevels_processesMultipleCandidatesIndependently() {
    LevelProgress candidateOne = levelProgress(1L);
    LevelProgress candidateTwo = levelProgress(2L);
    when(levelProgressRepository.findByStatusAndAutoTransitionAtLessThanEqual(
            LevelProgressStatus.ACTIVE, fixedNow))
        .thenReturn(List.of(candidateOne, candidateTwo));
    when(levelProgressRepository.tryAutoTransition(1L, fixedNow)).thenReturn(1);
    when(levelProgressRepository.tryAutoTransition(2L, fixedNow)).thenReturn(0);
    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(candidateOne));
    when(questProgressService.advanceAfterLevelCompleted(any()))
        .thenReturn(QuestProgressResponse.builder().build());

    levelAutoTransitionScheduler.autoTransitionDueLevels();

    verify(questProgressService).advanceAfterLevelCompleted(candidateOne);
    verify(questProgressService, never()).advanceAfterLevelCompleted(candidateTwo);
  }
}
