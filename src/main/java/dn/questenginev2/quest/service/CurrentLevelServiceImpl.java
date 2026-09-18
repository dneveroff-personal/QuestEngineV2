package dn.questenginev2.quest.service;

import dn.questenginev2.code.repository.CodeSubmissionRepository;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.ResourceNotFoundException;
import dn.questenginev2.hint.service.HintProgressService;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.quest.dto.CurrentLevelResponse;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class CurrentLevelServiceImpl implements CurrentLevelService {

  private final QuestProgressRepository questProgressRepository;
  private final LevelProgressRepository levelProgressRepository;
  private final CodeSubmissionRepository codeSubmissionRepository;
  private final HintProgressService hintProgressService;
  private final TeamMemberRepository teamMemberRepository;
  private final UserService userService;

  @Override
  public CurrentLevelResponse getCurrentLevel(Long questId, Long teamId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    QuestProgress questProgress =
        questProgressRepository
            .findByQuestIdAndTeamId(questId, teamId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Прогресс команды на квесте не найден: quest="
                            + questId
                            + ", team="
                            + teamId));

    validateTeamMemberOrAdmin(currentUser, questProgress.getTeam());

    LevelProgress levelProgress =
        levelProgressRepository
            .findByQuestProgressIdAndStatus(questProgress.getId(), LevelProgressStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "У команды нет активного уровня на этом квесте"));

    Level level = levelProgress.getLevel();
    long mainCodesSolved =
        codeSubmissionRepository.countDistinctSolvedCodeIndexes(levelProgress.getId());

    return CurrentLevelResponse.builder()
        .levelProgressId(levelProgress.getId())
        .levelProgressStatus(levelProgress.getStatus())
        .openedAt(levelProgress.getOpenedAt())
        .autoTransitionAt(levelProgress.getAutoTransitionAt())
        .levelId(level.getId())
        .orderIndex(level.getOrderIndex())
        .title(level.getTitle())
        .content(level.getContent())
        .requiredMainCodesCount(level.getRequiredMainCodesCount())
        .timeoutSeconds(level.getTimeoutSeconds())
        .mainCodesSolved(mainCodesSolved)
        .hints(hintProgressService.getVisibleHints(questId, teamId, auth))
        .build();
  }

  private void validateTeamMemberOrAdmin(User user, Team team) {
    if (user.getRole() == UserRole.ADMIN) {
      return;
    }
    boolean member = teamMemberRepository.findByUserAndTeam(user, team).isPresent();
    if (!member) {
      throw new ForbiddenOperationException("Только участник команды или ADMIN");
    }
  }
}
