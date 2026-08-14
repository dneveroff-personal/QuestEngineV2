package dn.questenginev2.quest.service;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.TeamNotFoundException;
import dn.questenginev2.quest.dto.QuestRegisterResponse;
import dn.questenginev2.quest.entity.*;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestRegistrationRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.entity.TeamRole;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Transactional
@AllArgsConstructor
public class QuestRegistrationServiceImpl implements QuestRegistrationService {

  private final QuestRegistrationRepository questRegistrationRepository;
  private final QuestRepository questRepository;
  private final QuestProgressService questProgressService;
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final QuestAuthorRepository questAuthorRepository;
  private final UserService userService;

  // ────── IMPLEMENTATIONS ───────────────────────────────────────────────────────────
  @Override
  public QuestRegisterResponse registerTeam(Long questId, Long teamId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);

    Quest quest = validateQuestExist(questId);
    validateQuestNotFinished(quest);

    Team team = validateTeamExist(teamId);
    validateTeamCaptain(currentUser, team);

    validateNoDuplicateRegistration(questId, teamId);

    QuestRegistration registration =
        QuestRegistration.builder()
            .quest(quest)
            .team(team)
            .status(RegistrationStatus.PENDING)
            .build();

    QuestRegistration savedRegistration = questRegistrationRepository.save(registration);
    return buildQuestRegisterResponse(savedRegistration);
  }

  @Override
  public List<QuestRegisterResponse> findAll(Long questId) {
    validateQuestExist(questId);

    return questRegistrationRepository.findByQuestId(questId).stream()
        .map(this::buildQuestRegisterResponse)
        .collect(Collectors.toList());
  }

  @Override
  public QuestRegisterResponse unregisterTeam(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);

    Team team = getCurrentUserTeam(currentUser);
    QuestRegistration registration =
        questRegistrationRepository
            .findByQuestIdAndTeamId(questId, team.getId())
            .orElseThrow(() -> new IllegalArgumentException("Регистрация не найдена"));

    validateRegistrationPending(registration);

    questRegistrationRepository.delete(registration);
    return buildQuestRegisterResponse(registration);
  }

  @Override
  @Transactional
  public QuestRegisterResponse approveTeam(Long questId, Long teamId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);

    validateQuestAuthor(currentUser, questId);
    validateTeamExist(teamId);

    QuestRegistration registration =
        questRegistrationRepository
            .findByQuestIdAndTeamId(questId, teamId)
            .orElseThrow(() -> new IllegalArgumentException("Регистрация не найдена"));

    validateRegistrationPending(registration);
    validateApprovedTeamsLimit(questId);

    registration.setStatus(RegistrationStatus.APPROVED);
    registration.setUpdatedAt(Instant.now());

    QuestRegistration savedRegistration = questRegistrationRepository.save(registration);

    Quest quest = validateQuestExist(questId);
    if (quest.getStatus() == QuestStatus.RUNNING) {
      questProgressService.createProgress(questId, teamId);
    }

    return buildQuestRegisterResponse(savedRegistration);
  }

  @Override
  public QuestRegisterResponse rejectTeam(Long teamId, Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);

    validateTeamExist(teamId);
    validateQuestExist(questId);

    QuestRegistration registration =
        questRegistrationRepository
            .findByTeamIdAndQuestIdAndStatus(teamId, questId, RegistrationStatus.PENDING)
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Активная заявка не найдена"));

    validateQuestAuthor(currentUser, registration.getQuest().getId());
    validateRegistrationPending(registration);

    registration.setStatus(RegistrationStatus.REJECTED);
    registration.setUpdatedAt(Instant.now());

    QuestRegistration savedRegistration = questRegistrationRepository.save(registration);
    return buildQuestRegisterResponse(savedRegistration);
  }

  // ────── VALIDATIONS ───────────────────────────────────────────────────────────
  private Quest validateQuestExist(Long questId) {
    return questRepository
        .findById(questId)
        .orElseThrow(() -> new IllegalArgumentException("Квест не найден: " + questId));
  }

  private void validateQuestNotFinished(Quest quest) {
    if (quest.getStatus() == QuestStatus.FINISHED) {
      throw new ForbiddenOperationException("Нельзя подать заявку на завершённый квест");
    }
  }

  private Team validateTeamExist(Long teamId) {
    return teamRepository
        .findById(teamId)
        .orElseThrow(() -> new IllegalArgumentException("Команда не найдена: " + teamId));
  }

  private void validateTeamCaptain(User user, Team team) {
    TeamMember teamMember =
        teamMemberRepository
            .findByUserAndTeam(user, team)
            .orElseThrow(
                () -> new ForbiddenOperationException("Пользователь не состоит в команде"));

    if (teamMember.getRole() != TeamRole.CAPTAIN) {
      throw new ForbiddenOperationException("Подать заявку может только капитан команды");
    }
  }

  private void validateNoDuplicateRegistration(Long questId, Long teamId) {
    if (questRegistrationRepository.existsByQuestIdAndTeamId(questId, teamId)) {
      throw new IllegalArgumentException("Команда уже подала заявку на этот квест");
    }
  }

  private void validateRegistrationPending(QuestRegistration registration) {
    if (registration.getStatus() != RegistrationStatus.PENDING) {
      throw new ForbiddenOperationException(
          "Можно отменить/подтвердить/отклонить только заявку в статусе PENDING");
    }
  }

  private void validateQuestAuthor(User user, Long questId) {
    if (user.getRole() != UserRole.ADMIN
        && !questAuthorRepository.existsByQuestIdAndUserId(questId, user.getId())) {
      throw new ForbiddenOperationException("Подтверждать заявки может только Автор квеста");
    }
  }

  private void validateApprovedTeamsLimit(Long questId) {
    Quest quest = validateQuestExist(questId);
    long approvedCount =
        questRegistrationRepository.countByQuestIdAndStatus(questId, RegistrationStatus.APPROVED);

    if (approvedCount >= quest.getMaximumTeams()) {
      throw new ForbiddenOperationException(
          "Достигнут лимит команд для этого квеста: " + quest.getMaximumTeams());
    }
  }

  private Team getCurrentUserTeam(User user) {
    TeamMember teamMember =
        teamMemberRepository
            .findByUser(user)
            .orElseThrow(() -> new TeamNotFoundException("Команда пользователя не найдена"));

    return teamMember.getTeam();
  }

  // ────── BUILDERS ───────────────────────────────────────────────────────────
  private QuestRegisterResponse buildQuestRegisterResponse(QuestRegistration registration) {
    return QuestRegisterResponse.builder()
        .questId(registration.getQuest().getId())
        .teamId(registration.getTeam().getId())
        .teamName(registration.getTeam().getName())
        .status(registration.getStatus())
        .build();
  }
}
