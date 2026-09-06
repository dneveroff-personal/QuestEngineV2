package dn.questenginev2.bonuspenalty.service;

import dn.questenginev2.bonuspenalty.entity.TimeAdjustmentType;
import dn.questenginev2.bonuspenalty.repository.ManualTimeAdjustmentRepository;
import dn.questenginev2.quest.entity.QuestProgress;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class BonusPenaltyServiceImpl implements BonusPenaltyService {

  private final ManualTimeAdjustmentRepository manualTimeAdjustmentRepository;

  @Override
  public long getAdjustmentSeconds(QuestProgress questProgress, TimeAdjustmentType type) {
    return manualTimeAdjustmentRepository.sumActiveSecondsByQuestProgressIdAndType(
        questProgress.getId(), type);
  }

  @Override
  public long getTotalAdjustmentSeconds(QuestProgress questProgress) {
    long bonus = getAdjustmentSeconds(questProgress, TimeAdjustmentType.BONUS);
    long penalty = getAdjustmentSeconds(questProgress, TimeAdjustmentType.PENALTY);

    return penalty - bonus;
  }
}
