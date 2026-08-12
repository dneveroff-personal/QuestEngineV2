package dn.questenginev2.quest.service;

import dn.questenginev2.quest.dto.QuestRegisterResponse;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface QuestRegistrationService {

  // ✓ команда может подать заявку
  // ✓ только капитан может подать заявку
  // ✓ нельзя подать дважды
  // ✓ нельзя подать заявку на несуществующий Quest
  // ✓ нельзя подать заявку на завершённый Quest
  QuestRegisterResponse registerTeam(Long questId, Long teamId, Authentication auth);

  List<QuestRegisterResponse> findAll(Long questId);

  // ✓ PENDING можно отменить
  QuestRegisterResponse unregisterTeam(Long questId, Authentication auth);

  // ✓ автор может APPROVE
  // ✓ нельзя APPROVE чужой Quest
  // ✓ нельзя изменить APPROVED
  // ✓ нельзя подтвердить сверх maximumTeams
  // ✓ можно подтвердить после старта Quest
  QuestRegisterResponse approveTeam(Long questId, Long teamId, Authentication auth);

  // ✓ автор может REJECT
  QuestRegisterResponse rejectTeam(Long teamId, Long questId, Authentication auth);
}
