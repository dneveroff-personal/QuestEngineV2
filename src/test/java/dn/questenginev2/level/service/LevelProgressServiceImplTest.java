package dn.questenginev2.level.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.level.dto.LevelProgressResponse;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LevelProgressServiceImplTest {

  @Mock private LevelProgressRepository levelProgressRepository;
  @Mock private LevelRepository levelRepository;
  @Mock private QuestProgressRepository questProgressRepository;
  @Mock private Clock clock;

  @InjectMocks private LevelProgressServiceImpl levelProgressService;

  private Quest quest;
  private QuestProgress questProgress;
  private Level level1;
  private LevelProgress activeLevelProgress;

  @BeforeEach
  void setUp() {
    Instant fixedNow = Instant.parse("2024-01-01T21:30:00Z");
    when(clock.instant()).thenReturn(fixedNow);

    Instant questStartTime = Instant.parse("2024-01-01T21:00:00Z");

    quest =
        Quest.builder()
            .id(1L)
            .title("Test Quest")
            .status(dn.questenginev2.quest.entity.QuestStatus.RUNNING)
            .startTime(questStartTime)
            .build();

    questProgress =
        QuestProgress.builder().id(1L).quest(quest).questStartedAt(questStartTime).build();

    level1 =
        Level.builder()
            .id(1L)
            .quest(quest)
            .title("Level 1")
            .orderIndex(1)
            .timeoutSeconds(3600) // 60 минут
            .build();
  }

  // ────── CREATE FIRST LEVEL PROGRESS ────────────────────────────────────────────

  @Test
  void createFirstLevelProgress_returnsActive_whenEnteredBeforeAutoTransition() {
    Instant enteredAt = Instant.parse("2024-01-01T21:30:00Z"); // 21:30

    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(level1));
    when(levelProgressRepository.existsByQuestProgressIdAndLevelId(1L, 1L)).thenReturn(false);

    LevelProgress saved =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(enteredAt)
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();
    when(levelProgressRepository.save(any(LevelProgress.class))).thenReturn(saved);

    LevelProgressResponse response = levelProgressService.createFirstLevelProgress(questProgress);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.ACTIVE);
    assertThat(response.getOpenedAt()).isEqualTo(enteredAt);
    assertThat(response.getAutoTransitionAt()).isEqualTo(Instant.parse("2024-01-01T22:00:00Z"));
    assertThat(response.getCompletedAt()).isNull();
  }

  @Test
  void createFirstLevelProgress_returnsAutoTransitioned_whenEnteredAfterAutoTransition() {
    Instant enteredAt = Instant.parse("2024-01-01T22:30:00Z"); // 22:30

    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(level1));
    when(levelProgressRepository.existsByQuestProgressIdAndLevelId(1L, 1L)).thenReturn(false);

    LevelProgress saved =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.AUTO_TRANSITIONED)
            .openedAt(enteredAt)
            .completedAt(enteredAt)
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();
    when(levelProgressRepository.save(any(LevelProgress.class))).thenReturn(saved);

    LevelProgressResponse response = levelProgressService.createFirstLevelProgress(questProgress);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.AUTO_TRANSITIONED);
    assertThat(response.getOpenedAt()).isEqualTo(enteredAt);
    assertThat(response.getAutoTransitionAt()).isEqualTo(Instant.parse("2024-01-01T22:00:00Z"));
    assertThat(response.getCompletedAt()).isEqualTo(enteredAt);
  }

  @Test
  void createFirstLevelProgress_throwsForbiddenOperationException_whenLevelAlreadyPlayed() {
    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(level1));
    when(levelProgressRepository.existsByQuestProgressIdAndLevelId(1L, 1L)).thenReturn(true);

    assertThatThrownBy(() -> levelProgressService.createFirstLevelProgress(questProgress))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("уже был сыгран");

    verify(levelProgressRepository, never()).save(any());
  }

  @Test
  void createFirstLevelProgress_throwsIllegalArgumentException_whenQuestProgressNotFound() {
    QuestProgress emptyProgress = new QuestProgress();
    assertThatThrownBy(() -> levelProgressService.createFirstLevelProgress(emptyProgress))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("QuestProgress не найден");

    verify(levelProgressRepository, never()).save(any());
  }

  @Test
  void createFirstLevelProgress_throwsIllegalArgumentException_whenLevelNotFound() {
    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> levelProgressService.createFirstLevelProgress(questProgress))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Уровень не найден");

    verify(levelProgressRepository, never()).save(any());
  }

  // ────── COMPLETE LEVEL ─────────────────────────────────────────────────────────

  @Test
  void completeLevel_changesToCompleted_whenCodesCompletion() {
    activeLevelProgress =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();

    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(activeLevelProgress));

    Instant completedAt = Instant.parse("2024-01-01T21:45:00Z");
    LevelProgress completed =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.COMPLETED)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .completedAt(completedAt)
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();
    when(levelProgressRepository.save(any(LevelProgress.class))).thenReturn(completed);

    LevelProgressResponse response = levelProgressService.completeLevel(1L);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.COMPLETED);
    assertThat(response.getCompletedAt()).isEqualTo(completedAt);
  }

  @Test
  void completeLevel_changesToAutoTransitioned_whenAutoTransitionCompletion() {
    activeLevelProgress =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();

    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(activeLevelProgress));

    when(clock.instant()).thenReturn(Instant.parse("2024-01-01T22:30:00Z"));

    Instant completedAt = Instant.parse("2024-01-01T22:30:00Z");
    LevelProgress autoTransitioned =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.AUTO_TRANSITIONED)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .completedAt(completedAt)
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();
    when(levelProgressRepository.save(any(LevelProgress.class))).thenReturn(autoTransitioned);

    LevelProgressResponse response = levelProgressService.autoTransitionLevel(1L);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.AUTO_TRANSITIONED);
    assertThat(response.getCompletedAt()).isEqualTo(completedAt);
  }

  @Test
  void completeLevel_throwsForbiddenOperationException_whenNotActive() {
    LevelProgress completedLevel =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.COMPLETED)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .completedAt(Instant.parse("2024-01-01T21:45:00Z"))
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();

    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(completedLevel));

    assertThatThrownBy(() -> levelProgressService.completeLevel(1L))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("ACTIVE");

    verify(levelProgressRepository, never()).save(any());
  }

  // ────── AUTO TRANSITION LEVEL ──────────────────────────────────────────────────

  @Test
  void autoTransitionLevel_changesToAutoTransitioned_whenTimePassed() {
    activeLevelProgress =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();

    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(activeLevelProgress));

    when(clock.instant()).thenReturn(Instant.parse("2024-01-01T22:30:00Z"));

    Instant now = Instant.parse("2024-01-01T22:30:00Z");
    LevelProgress autoTransitioned =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.AUTO_TRANSITIONED)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .completedAt(now)
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();
    when(levelProgressRepository.save(any(LevelProgress.class))).thenReturn(autoTransitioned);

    // Используем try-catch для Instant.now() или просто проверяем что completedAt установлен
    // В тестах мы не можем легко подменить Instant.now(), поэтому проверяем через ArgumentCaptor
    ArgumentCaptor<LevelProgress> captor = ArgumentCaptor.forClass(LevelProgress.class);
    when(levelProgressRepository.save(captor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LevelProgressResponse response = levelProgressService.autoTransitionLevel(1L);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.AUTO_TRANSITIONED);
    assertThat(response.getCompletedAt()).isNotNull();

    LevelProgress saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo(LevelProgressStatus.AUTO_TRANSITIONED);
    assertThat(saved.getCompletedAt()).isNotNull();
  }

  @Test
  void autoTransitionLevel_returnsActive_whenTimeNotPassed() {
    Instant futureAutoTransition = Instant.now().plusSeconds(3600);

    activeLevelProgress =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(Instant.now())
            .autoTransitionAt(futureAutoTransition)
            .build();

    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(activeLevelProgress));

    LevelProgressResponse response = levelProgressService.autoTransitionLevel(1L);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.ACTIVE);
    assertThat(response.getCompletedAt()).isNull();

    verify(levelProgressRepository, never()).save(any());
  }

  @Test
  void autoTransitionLevel_changesToAutoTransitioned_whenTimeExactlyEqualsAutoTransitionAt() {
    activeLevelProgress =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();

    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(activeLevelProgress));

    Instant exactlyAtDeadline = Instant.parse("2024-01-01T22:00:00Z");
    when(clock.instant()).thenReturn(exactlyAtDeadline);

    LevelProgress autoTransitioned =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.AUTO_TRANSITIONED)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .completedAt(exactlyAtDeadline)
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();
    when(levelProgressRepository.save(any(LevelProgress.class))).thenReturn(autoTransitioned);

    LevelProgressResponse response = levelProgressService.autoTransitionLevel(1L);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.AUTO_TRANSITIONED);
    assertThat(response.getCompletedAt()).isEqualTo(exactlyAtDeadline);
  }

  @Test
  void autoTransitionLevel_returnsActiveAndDoesNotSave_whenAutoTransitionAtIsNull() {
    activeLevelProgress =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .autoTransitionAt(null)
            .build();

    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(activeLevelProgress));

    LevelProgressResponse response = levelProgressService.autoTransitionLevel(1L);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.ACTIVE);
    assertThat(response.getCompletedAt()).isNull();

    verify(levelProgressRepository, never()).save(any());
  }

  @Test
  void autoTransitionLevel_throwsForbiddenOperationException_whenNotActive() {
    LevelProgress completedLevel =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.COMPLETED)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .completedAt(Instant.parse("2024-01-01T21:45:00Z"))
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();

    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(completedLevel));

    assertThatThrownBy(() -> levelProgressService.autoTransitionLevel(1L))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("ACTIVE");

    verify(levelProgressRepository, never()).save(any());
  }

  // ────── CREATE FIRST LEVEL PROGRESS - EDGE CASES ───────────────────────────────

  @Test
  void createFirstLevelProgress_throwsForbiddenOperationException_whenLevelBelongsToAnotherQuest() {
    Quest questB =
        Quest.builder()
            .id(2L)
            .title("Quest B")
            .status(dn.questenginev2.quest.entity.QuestStatus.RUNNING)
            .startTime(Instant.parse("2024-01-01T21:00:00Z"))
            .build();
    QuestProgress questProgressB =
        QuestProgress.builder()
            .id(2L)
            .quest(questB)
            .questStartedAt(Instant.parse("2024-01-01T21:00:00Z"))
            .build();
    Level levelB =
        Level.builder()
            .id(2L)
            .quest(questB)
            .title("Level B")
            .orderIndex(1)
            .timeoutSeconds(3600)
            .build();

    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(levelB));

    assertThatThrownBy(() -> levelProgressService.createFirstLevelProgress(questProgress))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("не принадлежит QuestProgress");

    verify(levelProgressRepository, never()).save(any());
  }

  @Test
  void createFirstLevelProgress_returnsActiveWithNullAutoTransition_whenTimeoutSecondsIsNull() {
    Level levelNoTimeout =
        Level.builder()
            .id(1L)
            .quest(quest)
            .title("Level No Timeout")
            .orderIndex(1)
            .timeoutSeconds(null)
            .build();

    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(levelNoTimeout));
    when(levelProgressRepository.existsByQuestProgressIdAndLevelId(1L, 1L)).thenReturn(false);

    ArgumentCaptor<LevelProgress> captor = ArgumentCaptor.forClass(LevelProgress.class);
    when(levelProgressRepository.save(captor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LevelProgressResponse response = levelProgressService.createFirstLevelProgress(questProgress);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.ACTIVE);
    assertThat(response.getAutoTransitionAt()).isNull();

    LevelProgress saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo(LevelProgressStatus.ACTIVE);
    assertThat(saved.getAutoTransitionAt()).isNull();
  }

  @Test
  void
      createFirstLevelProgress_returnsAutoTransitioned_whenOpenedAtExactlyEqualsAutoTransitionAt() {
    Instant questStartedAt = Instant.parse("2024-01-01T21:00:00Z");
    questProgress =
        QuestProgress.builder().id(1L).quest(quest).questStartedAt(questStartedAt).build();

    Instant autoTransitionAt = questStartedAt.plusSeconds(3600); // 22:00
    Instant openedAt = autoTransitionAt; // exactly at deadline

    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(level1));
    when(levelProgressRepository.existsByQuestProgressIdAndLevelId(1L, 1L)).thenReturn(false);
    when(clock.instant()).thenReturn(openedAt);

    ArgumentCaptor<LevelProgress> captor = ArgumentCaptor.forClass(LevelProgress.class);
    when(levelProgressRepository.save(captor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LevelProgressResponse response = levelProgressService.createFirstLevelProgress(questProgress);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.AUTO_TRANSITIONED);
    assertThat(response.getOpenedAt()).isEqualTo(openedAt);
    assertThat(response.getCompletedAt()).isEqualTo(openedAt);

    LevelProgress saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo(LevelProgressStatus.AUTO_TRANSITIONED);
    assertThat(saved.getCompletedAt()).isEqualTo(openedAt);
  }

  @Test
  void createFirstLevelProgress_returnsAutoTransitioned_whenOpenedAtAfterAutoTransitionAt() {
    Instant questStartedAt = Instant.parse("2024-01-01T21:00:00Z");
    questProgress =
        QuestProgress.builder().id(1L).quest(quest).questStartedAt(questStartedAt).build();

    Instant autoTransitionAt = questStartedAt.plusSeconds(3600); // 22:00
    Instant openedAt = autoTransitionAt.plusSeconds(30); // 22:00:30, after deadline

    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(level1));
    when(levelProgressRepository.existsByQuestProgressIdAndLevelId(1L, 1L)).thenReturn(false);

    when(clock.instant()).thenReturn(openedAt);

    ArgumentCaptor<LevelProgress> captor = ArgumentCaptor.forClass(LevelProgress.class);
    when(levelProgressRepository.save(captor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LevelProgressResponse response = levelProgressService.createFirstLevelProgress(questProgress);

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.AUTO_TRANSITIONED);
    assertThat(response.getOpenedAt()).isEqualTo(openedAt);
    assertThat(response.getCompletedAt()).isEqualTo(openedAt);

    LevelProgress saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo(LevelProgressStatus.AUTO_TRANSITIONED);
    assertThat(saved.getCompletedAt()).isEqualTo(openedAt);
  }

  @Test
  void createFirstLevelProgress_usesQuestStartedAtForAutoTransitionCalculation() {
    Instant questStartedAt = Instant.parse("2024-01-01T21:00:00Z");
    questProgress =
        QuestProgress.builder().id(1L).quest(quest).questStartedAt(questStartedAt).build();

    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(level1));
    when(levelProgressRepository.existsByQuestProgressIdAndLevelId(1L, 1L)).thenReturn(false);

    ArgumentCaptor<LevelProgress> captor = ArgumentCaptor.forClass(LevelProgress.class);
    when(levelProgressRepository.save(captor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    levelProgressService.createFirstLevelProgress(questProgress);

    LevelProgress saved = captor.getValue();
    // autoTransitionAt should be questStartedAt + timeoutSeconds (3600s) = 22:00:00Z
    assertThat(saved.getAutoTransitionAt()).isEqualTo(questStartedAt.plusSeconds(3600));
  }
}
