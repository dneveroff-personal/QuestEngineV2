package dn.questenginev2.bonuspenalty.service;

import dn.questenginev2.bonuspenalty.entity.TimeAdjustmentType;
import dn.questenginev2.bonuspenalty.repository.ManualTimeAdjustmentRepository;
import dn.questenginev2.code.entity.CodeSubmissionResult;
import dn.questenginev2.code.repository.CodeSubmissionRepository;
import dn.questenginev2.hint.entity.HintType;
import dn.questenginev2.hint.repository.HintProgressRepository;
import dn.questenginev2.quest.entity.QuestProgress;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Агрегирует три источника bonus/penalty (ADR-0007, bonus-penalty.md):
 * ManualTimeAdjustment (ручная корректировка автора), CodeSubmission
 * (BONUS/PENALTY-коды), HintProgress (BONUS/PENALTY-подсказки, ADR-0021).
 *
 * Намеренно НЕ хранит агрегат как поле — считает при каждом обращении
 * (ADR-0007: "агрегация суммированием при чтении, а не инкремент
 * изменяемого поля" — исключает lost-update гонки, см.
 * concurrency-scenarios.md Сценарий 4).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BonusPenaltyServiceImpl implements BonusPenaltyService {

  private final ManualTimeAdjustmentRepository manualTimeAdjustmentRepository;
  private final CodeSubmissionRepository codeSubmissionRepository;
  private final HintProgressRepository hintProgressRepository;

  @Override
  public long getAdjustmentSeconds(QuestProgress questProgress, TimeAdjustmentType type) {
    long manual =
        manualTimeAdjustmentRepository.sumActiveSecondsByQuestProgressIdAndType(
            questProgress.getId(), type);

    CodeSubmissionResult codeResult =
        type == TimeAdjustmentType.BONUS
            ? CodeSubmissionResult.CORRECT_BONUS
            : CodeSubmissionResult.CORRECT_PENALTY;
    long code =
        codeSubmissionRepository.sumEffectSecondsByQuestProgressIdAndResult(
            questProgress.getId(), codeResult);

    HintType hintType = type == TimeAdjustmentType.BONUS ? HintType.BONUS : HintType.PENALTY;
    long hint =
        hintProgressRepository.sumEffectSecondsByQuestProgressIdAndHintType(
            questProgress.getId(), hintType);

    return manual + code + hint;
  }

  @Override
  public long getTotalAdjustmentSeconds(QuestProgress questProgress) {
    long bonus = getAdjustmentSeconds(questProgress, TimeAdjustmentType.BONUS);
    long penalty = getAdjustmentSeconds(questProgress, TimeAdjustmentType.PENALTY);

    // Знак: bonus уменьшает итоговое время, penalty увеличивает (ADR-0007).
    return penalty - bonus;
  }
}
