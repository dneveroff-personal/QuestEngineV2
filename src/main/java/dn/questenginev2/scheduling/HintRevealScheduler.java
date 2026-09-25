package dn.questenginev2.scheduling;

import dn.questenginev2.gameplay.event.GameplayEventPublisher;
import dn.questenginev2.gameplay.event.GameplayEventType;
import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.hint.entity.HintProgress;
import dn.questenginev2.hint.entity.HintType;
import dn.questenginev2.hint.repository.HintProgressRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
 * <p>ADR-0021: этот планировщик показывает только подсказки типа {@code REGULAR}. Подсказки
 * {@code BONUS}/{@code PENALTY} не показываются автоматически — команда должна явно "взять" их
 * (см. {@code HintProgressServiceImpl#takeHint}).
 *
 * <p>ADR-022: после успешного показа публикует {@code HINT_REVEALED} (notify + REST refetch).
 */
@Component
@AllArgsConstructor
public class HintRevealScheduler {

  private final LevelProgressRepository levelProgressRepository;
  private final HintRepository hintRepository;
  private final HintProgressRepository hintProgressRepository;
  private final GameplayEventPublisher gameplayEventPublisher;
  private final Clock clock;

  @Autowired
  public HintRevealScheduler(
      LevelProgressRepository levelProgressRepository,
      HintRepository hintRepository,
      HintProgressRepository hintProgressRepository,
      GameplayEventPublisher gameplayEventPublisher) {
    this(
        levelProgressRepository,
        hintRepository,
        hintProgressRepository,
        gameplayEventPublisher,
        Clock.systemUTC());
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

    Long questProgressId = levelProgress.getQuestProgress().getId();
    Long questId = levelProgress.getQuestProgress().getQuest().getId();
    Long levelId = levelProgress.getLevel().getId();

    for (Hint hint : hints) {
      if (hint.getType() != HintType.REGULAR) {
        // ADR-0021: BONUS/PENALTY показываются только по явному взятию командой, не Job 3.
        continue;
      }
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
        gameplayEventPublisher.publish(
            GameplayEventType.HINT_REVEALED,
            questId,
            questProgressId,
            levelProgress.getId(),
            Map.of(
                "hintId", hint.getId(),
                "hintType", HintType.REGULAR.name(),
                "levelId", levelId));
      } catch (DataIntegrityViolationException alreadyShownConcurrently) {
        // Идемпотентно: подсказка уже показана параллельным выполнением job — defense-in-depth.
      }
    }
  }
}
