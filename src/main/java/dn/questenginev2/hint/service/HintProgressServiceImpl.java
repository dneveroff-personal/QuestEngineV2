package dn.questenginev2.hint.service;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.hint.dto.HintProgressResponse;
import dn.questenginev2.hint.entity.HintProgress;
import dn.questenginev2.hint.repository.HintProgressRepository;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Transactional
@AllArgsConstructor
public class HintProgressServiceImpl implements HintProgressService {

  private final HintProgressRepository hintProgressRepository;
  private final QuestProgressRepository questProgressRepository;
  private final LevelProgressRepository levelProgressRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final UserService userService;

  @Override
  public List<HintProgressResponse> getShownHints(Long questId, Long teamId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);

    QuestProgress questProgress =
        questProgressRepository
            .findByQuestIdAndTeamId(questId, teamId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Прогресс команды не найден: questId=" + questId + ", teamId=" + teamId));

    validateTeamMembership(currentUser, questProgress.getTeam());

    Optional<LevelProgress> activeLevelProgress =
        levelProgressRepository.findByQuestProgressIdAndStatus(
            questProgress.getId(), LevelProgressStatus.ACTIVE);

    if (activeLevelProgress.isEmpty()) {
      return List.of();
    }

    return hintProgressRepository
        .findByLevelProgressIdOrderByShownAt(activeLevelProgress.get().getId())
        .stream()
        .map(this::buildHintProgressResponse)
        .toList();
  }

  private void validateTeamMembership(User user, Team team) {
    if (teamMemberRepository.findByUserAndTeam(user, team).isEmpty()) {
      throw new ForbiddenOperationException("Видеть подсказки может только участник этой команды");
    }
  }

  private HintProgressResponse buildHintProgressResponse(HintProgress hintProgress) {
    return HintProgressResponse.builder()
        .hintId(hintProgress.getHint().getId())
        .orderIndex(hintProgress.getHint().getOrderIndex())
        .content(hintProgress.getHint().getContent())
        .type(hintProgress.getHint().getType())
        .bonusPenaltySeconds(hintProgress.getHint().getBonusPenaltySeconds())
        .shownAt(hintProgress.getShownAt())
        .build();
  }
}
