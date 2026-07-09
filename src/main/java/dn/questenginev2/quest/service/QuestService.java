package dn.questenginev2.quest.service;

import dn.questenginev2.quest.dto.CreateQuestRequest;
import dn.questenginev2.quest.dto.QuestResponse;
import org.springframework.security.core.Authentication;

public interface QuestService {

    QuestResponse createQuest(CreateQuestRequest request, Authentication auth);

    QuestResponse getQuestById(Long questId);
}
