package dn.questenginev2.level.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.common.exceptions.ConflictException;
import dn.questenginev2.common.exceptions.ResourceNotFoundException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for LevelProgressServiceImpl. Auto-transition concurrency is covered by
 * LevelAutoTransitionScheduler / tryAutoTransition IT (deprecated service method removed).
 */
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
            .timeoutSeconds(3600)
            .build();
  }

  @Test
  void createFirstLevelProgress_returnsActive_whenEnteredBeforeAutoTransition() {
    Instant enteredAt = Instant.parse("2024-01-01T21:30:00Z");
    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(level1));
    LevelProgress saved =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(enteredAt)
            .autoTransitionAt(Instant.parse("2024-01-01T22:00:00Z"))
            .build();
    when(levelProgressRepository.saveAndFlush(any(LevelProgress.class))).thenReturn(saved);

    LevelProgressResponse response = levelProgressService.createFirstLevelProgress(questProgress);

    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.ACTIVE);
    assertThat(response.getCompletedAt()).isNull();
  }

  @Test
  void createFirstLevelProgress_returnsAutoTransitioned_whenEnteredAfterAutoTransition() {
    Instant enteredAt = Instant.parse("2024-01-01T22:30:00Z");
    when(clock.instant()).thenReturn(enteredAt);
    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(level1));
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
    when(levelProgressRepository.saveAndFlush(any(LevelProgress.class))).thenReturn(saved);

    LevelProgressResponse response = levelProgressService.createFirstLevelProgress(questProgress);

    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.AUTO_TRANSITIONED);
  }

  @Test
  void createFirstLevelProgress_returnsExisting_whenAlreadyCreatedConcurrently() {
    when(levelRepository.findByQuestIdAndOrderIndex(1L, 1)).thenReturn(Optional.of(level1));
    when(levelProgressRepository.saveAndFlush(any()))
        .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));
    LevelProgress existing =
        LevelProgress.builder()
            .id(99L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.ACTIVE)
            .build();
    when(levelProgressRepository.findByQuestProgressIdAndLevelId(1L, 1L))
        .thenReturn(Optional.of(existing));

    LevelProgressResponse response = levelProgressService.createFirstLevelProgress(questProgress);

    assertThat(response).isNotNull();
    verify(levelProgressRepository, never()).save(any());
  }

  @Test
  void createFirstLevelProgress_throws_whenQuestProgressInvalid() {
    assertThatThrownBy(() -> levelProgressService.createFirstLevelProgress(new QuestProgress()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void completeLevel_changesToCompleted() {
    LevelProgress active =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(Instant.parse("2024-01-01T21:30:00Z"))
            .build();
    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(active));
    Instant completedAt = Instant.parse("2024-01-01T21:45:00Z");
    when(clock.instant()).thenReturn(completedAt);
    when(levelProgressRepository.save(any(LevelProgress.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    LevelProgressResponse response = levelProgressService.completeLevel(1L);

    assertThat(response.getStatus()).isEqualTo(LevelProgressStatus.COMPLETED);
  }

  @Test
  void completeLevel_throws_whenNotActive() {
    LevelProgress completed =
        LevelProgress.builder()
            .id(1L)
            .questProgress(questProgress)
            .level(level1)
            .status(LevelProgressStatus.COMPLETED)
            .build();
    when(levelProgressRepository.findById(1L)).thenReturn(Optional.of(completed));

    assertThatThrownBy(() -> levelProgressService.completeLevel(1L))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("ACTIVE");
  }
}
