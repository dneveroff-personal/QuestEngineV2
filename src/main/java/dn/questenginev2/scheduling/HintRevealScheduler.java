package dn.questenginev2.scheduling;

import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.hint.entity.HintProgress;
import dn.questenginev2.hint.repository.HintProgressRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job 3 (docs/01-domain/hint-progress.md, ADR-0020): auto-reveal подсказок. Для каждого активного
 * LevelProgress и каждой подсказки его уровня — если {@code openedAt + delaySeconds <= now} и
 * подсказка ещё не показана — создаёт {@code HintProgress}.
 *
 * <p>В отличие от Job 1/Job 2, здесь нет конкурирующего пути (подсказка не может быть "показана"
 * никаким другим способом, кроме этого планировщика) — идемпотентность обеспечивается уникальным
 * индексом (level_progress_id, hint_id) и перехватом нарушения как defense-in-depth, а не как
 * разрешение гонки между двумя разными путями (сравни со Сценарием 5).
 *
 * <p>Начисление эффекта BONUS/PENALTY-подсказки к итоговому времени команды — вне scope: здесь
 * только фиксируется факт показа, агрегация — отдельная фича (см. roadmap/backlog.md, п. 6,
 * ADR-0007).
 */
@Component
@AllArgsConstructor
public class HintRevealScheduler {

  private final LevelProgressRepository levelProgressRepository;
  private final HintRepository hintRepository;
  private final HintProgressRepository hintProgressRepository;
  private final Clock clock;

  @Autowired
  public HintRevealScheduler(
      LevelProgressRepository levelProgressRepository,
      HintRepository hintRepository,
      HintProgressRepository hintProgressRepository) {
    this(levelProgressRepository, hintRepository, hintProgressRepository, Clock.systemUTC());
  }

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void revealDueHints() {
    Instant now = clock.instant();
    List<LevelProgress> activeLevelProgresses =
        levelProgressRepository.findByStatus(LevelProgressStatus.ACTIVE);

    for (LevelProgress levelProgress : activeLevelProgresses) {
      revealDueHintsForLevelProgress(levelProgress, now);
    }
  }

  private void revealDueHintsForLevelProgress(LevelProgress levelProgress, Instant now) {
    List<Hint> hints =
        hintRepository.findByLevelIdOrderByOrderIndex(levelProgress.getLevel().getId());
    if (hints.isEmpty()) {
      return;
    }

    Set<Long> alreadyShownHintIds =
        hintProgressRepository.findByLevelProgressIdOrderByShownAt(levelProgress.getId()).stream()
            .map(hintProgress -> hintProgress.getHint().getId())
            .collect(Collectors.toSet());

    for (Hint hint : hints) {
      if (alreadyShownHintIds.contains(hint.getId())) {
        continue;
      }

      Instant hintAvailableAt = levelProgress.getOpenedAt().plusSeconds(hint.getDelaySeconds());
      if (now.isBefore(hintAvailableAt)) {
        continue;
      }

      try {
        hintProgressRepository.saveAndFlush(
            HintProgress.builder().levelProgress(levelProgress).hint(hint).shownAt(now).build());
      } catch (DataIntegrityViolationException alreadyShownConcurrently) {
        // Идемпотентно: подсказка уже показана параллельным выполнением job — defense-in-depth.
      }
    }
  }
}
