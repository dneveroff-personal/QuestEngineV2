package dn.questenginev2.bonuspenalty.service;

import dn.questenginev2.bonuspenalty.entity.TimeAdjustmentType;
import dn.questenginev2.quest.entity.QuestProgress;

public interface BonusPenaltyService {

  long getAdjustmentSeconds(QuestProgress questProgress, TimeAdjustmentType type);

  long getTotalAdjustmentSeconds(QuestProgress questProgress);
}
