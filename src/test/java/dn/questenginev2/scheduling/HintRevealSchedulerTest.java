package dn.questenginev2.scheduling;

import static org.mockito.Mockito.*;

import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.hint.entity.HintProgress;
import dn.questenginev2.hint.repository.HintProgressRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class HintRevealSchedulerTest {

  @Mock private LevelProgressRepository levelProgressRepository;
  @Mock private HintRepository hintRepository;
  @Mock private HintProgressRepository hintProgressRepository;
  @Mock private Clock clock;

  @InjectMocks private HintRevealScheduler hintRevealScheduler;

  private final Instant fixedNow = Instant.parse("2026-08-24T21:10:00Z");

  private LevelProgress levelProgress;
  private Level level;

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(fixedNow);

    level = Level.builder().id(1000L).title("L1").orderIndex(1).build();
    levelProgress =
        LevelProgress.builder()
            .id(2000L)
            .level(level)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(fixedNow.minusSeconds(120))
            .build();
  }

  @Test
  void revealDueHints_doesNothing_whenNoActiveLevelProgresses() {
    when(levelProgressRepository.findByStatus(LevelProgressStatus.ACTIVE)).thenReturn(List.of());

    hintRevealScheduler.revealDueHints();

    verify(hintProgressRepository, never()).saveAndFlush(any());
  }

  @Test
  void revealDueHints_skipsLevel_whenNoHintsConfigured() {
    when(levelProgressRepository.findByStatus(LevelProgressStatus.ACTIVE))
        .thenReturn(List.of(levelProgress));
    when(hintRepository.findByLevelIdOrderByOrderIndex(1000L)).thenReturn(List.of());

    hintRevealScheduler.revealDueHints();

    verify(hintProgressRepository, never()).saveAndFlush(any());
  }

  @Test
  void revealDueHints_revealsHint_whenDelayElapsedAndNotYetShown() {
    // openedAt = now - 120s, delaySeconds = 60 -> hintAvailableAt = now - 60s <= now: должна быть
    // показана.
    Hint hint =
        Hint.builder().id(1L).level(level).orderIndex(1).delaySeconds(60).content("Hint").build();
    when(levelProgressRepository.findByStatus(LevelProgressStatus.ACTIVE))
        .thenReturn(List.of(levelProgress));
    when(hintRepository.findByLevelIdOrderByOrderIndex(1000L)).thenReturn(List.of(hint));
    when(hintProgressRepository.findByLevelProgressIdOrderByShownAt(2000L)).thenReturn(List.of());

    hintRevealScheduler.revealDueHints();

    ArgumentCaptor<HintProgress> captor = ArgumentCaptor.forClass(HintProgress.class);
    verify(hintProgressRepository).saveAndFlush(captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getHint()).isEqualTo(hint);
    org.assertj.core.api.Assertions.assertThat(captor.getValue().getShownAt()).isEqualTo(fixedNow);
  }

  @Test
  void revealDueHints_doesNotReveal_whenDelayNotYetElapsed() {
    // delaySeconds = 300 -> hintAvailableAt = openedAt + 300s = now + 180s (в будущем).
    Hint hint =
        Hint.builder().id(1L).level(level).orderIndex(1).delaySeconds(300).content("Hint").build();
    when(levelProgressRepository.findByStatus(LevelProgressStatus.ACTIVE))
        .thenReturn(List.of(levelProgress));
    when(hintRepository.findByLevelIdOrderByOrderIndex(1000L)).thenReturn(List.of(hint));
    when(hintProgressRepository.findByLevelProgressIdOrderByShownAt(2000L)).thenReturn(List.of());

    hintRevealScheduler.revealDueHints();

    verify(hintProgressRepository, never()).saveAndFlush(any());
  }

  @Test
  void revealDueHints_skipsHint_whenAlreadyShown() {
    Hint hint =
        Hint.builder().id(1L).level(level).orderIndex(1).delaySeconds(60).content("Hint").build();
    HintProgress existingProgress =
        HintProgress.builder()
            .id(99L)
            .levelProgress(levelProgress)
            .hint(hint)
            .shownAt(fixedNow)
            .build();
    when(levelProgressRepository.findByStatus(LevelProgressStatus.ACTIVE))
        .thenReturn(List.of(levelProgress));
    when(hintRepository.findByLevelIdOrderByOrderIndex(1000L)).thenReturn(List.of(hint));
    when(hintProgressRepository.findByLevelProgressIdOrderByShownAt(2000L))
        .thenReturn(List.of(existingProgress));

    hintRevealScheduler.revealDueHints();

    verify(hintProgressRepository, never()).saveAndFlush(any());
  }

  @Test
  void revealDueHints_toleratesConcurrentDuplicate_whenSaveAndFlushThrows() {
    Hint hint =
        Hint.builder().id(1L).level(level).orderIndex(1).delaySeconds(60).content("Hint").build();
    when(levelProgressRepository.findByStatus(LevelProgressStatus.ACTIVE))
        .thenReturn(List.of(levelProgress));
    when(hintRepository.findByLevelIdOrderByOrderIndex(1000L)).thenReturn(List.of(hint));
    when(hintProgressRepository.findByLevelProgressIdOrderByShownAt(2000L)).thenReturn(List.of());
    when(hintProgressRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    // Не должно бросить исключение наружу.
    hintRevealScheduler.revealDueHints();
  }
}
