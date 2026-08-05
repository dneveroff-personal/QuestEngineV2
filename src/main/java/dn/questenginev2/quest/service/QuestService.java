package dn.questenginev2.quest.service;

import dn.questenginev2.quest.dto.CreateQuestRequest;
import dn.questenginev2.quest.dto.QuestResponse;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.user.entity.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface QuestService {

  QuestResponse createQuest(CreateQuestRequest request, Authentication auth);

  QuestResponse getQuestById(Long questId);

  List<QuestResponse> getAllByAuthorId(Long authorId);

  QuestResponse updateQuest(Long questId, @Valid CreateQuestRequest request, Authentication auth);

  void delete(Long questId, Authentication auth);

  Quest validateQuestExist(Long questId);

  void validateAuthorOrAdmin(User user);

  void validateQuestAuthor(User user, Long questId);
}
