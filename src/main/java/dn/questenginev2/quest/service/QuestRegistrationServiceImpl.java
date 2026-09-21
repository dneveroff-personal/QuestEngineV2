package dn.questenginev2.quest.service;

import dn.questenginev2.common.exceptions.ConflictException;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.ResourceNotFoundException;
import dn.questenginev2.quest.dto.QuestRegisterResponse;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestRegistration;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.RegistrationStatus;
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
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestRegistrationServiceImpl implements QuestRegistrationService {

  private final QuestRegistrationRepository questRegistrationRepository;
  private final QuestRepository questRepository;
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final QuestAuthorRepository questAuthorRepository;
  private final UserService userService;

  @Override
  public QuestRegisterResponse registerTeam(Long questId, Long teamId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);

    Quest quest = validateQuestExist(questId);
    validateQuestAcceptsRegistration(quest);

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
            .orElseThrow(() -> new ResourceNotFoundException("Регистрация не найдена"));

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
            .orElseThrow(() -> new ResourceNotFoundException("Регистрация не найдена"));

    if (registration.getStatus() != RegistrationStatus.PENDING) {
      throw new ConflictException("Можно подтвердить только PENDING регистрацию");
    }

    Quest quest = registration.getQuest();
    if (Boolean.TRUE.equals(quest.getArchived())) {
      throw new ConflictException("Нельзя подтверждать регистрацию на архивный квест");
    }

    long approved =
        questRegistrationRepository.countByQuestIdAndStatus(questId, RegistrationStatus.APPROVED);
    if (approved >= quest.getMaximumTeams()) {
      throw new ConflictException("Достигнут лимит команд на квест");
    }

    registration.setStatus(RegistrationStatus.APPROVED);
    return buildQuestRegisterResponse(questRegistrationRepository.save(registration));
  }

  @Override
  public QuestRegisterResponse rejectTeam(Long teamId, Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    validateQuestAuthor(currentUser, questId);

    QuestRegistration registration =
        questRegistrationRepository
            .findByQuestIdAndTeamId(questId, teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Регистрация не найдена"));

    if (registration.getStatus() != RegistrationStatus.PENDING) {
      throw new ConflictException("Можно отклонить только PENDING регистрацию");
    }

    registration.setStatus(RegistrationStatus.REJECTED);
    return buildQuestRegisterResponse(questRegistrationRepository.save(registration));
  }

  private void validateQuestAcceptsRegistration(Quest quest) {
    if (Boolean.TRUE.equals(quest.getArchived())) {
      throw new ConflictException("Нельзя подавать заявку на архивный квест");
    }
    if (quest.getStatus() != QuestStatus.REGISTRATION && quest.getStatus() != QuestStatus.RUNNING) {
      throw new ConflictException(
          "Подать заявку можно только пока квест в статусе REGISTRATION или RUNNING (поздняя"
              + " регистрация)");
    }
  }

  private Quest validateQuestExist(Long questId) {
    return questRepository
        .findById(questId)
        .orElseThrow(() -> new ResourceNotFoundException("Квест не найден: " + questId));
  }

  private Team validateTeamExist(Long teamId) {
    return teamRepository
        .findById(teamId)
        .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена: " + teamId));
  }

  private void validateTeamCaptain(User user, Team team) {
    TeamMember teamMember =
        teamMemberRepository
            .findByUserAndTeam(user, team)
            .orElseThrow(
                () -> new ForbiddenOperationException("Пользователь не состоит в команде"));

    if (teamMember.getRole() != TeamRole.CAPTAIN) {
      throw new ForbiddenOperationException("Только капитан может подать заявку на квест");
    }
  }

  private void validateNoDuplicateRegistration(Long questId, Long teamId) {
    if (questRegistrationRepository.existsByQuestIdAndTeamId(questId, teamId)) {
      throw new ConflictException("Команда уже зарегистрирована на этот квест");
    }
  }

  private void validateRegistrationPending(QuestRegistration registration) {
    if (registration.getStatus() != RegistrationStatus.PENDING) {
      throw new ConflictException("Отменить можно только PENDING регистрацию");
    }
  }

  private void validateQuestAuthor(User user, Long questId) {
    if (user.getRole() == UserRole.ADMIN) {
      return;
    }
    if (!questAuthorRepository.existsByQuestIdAndUserId(questId, user.getId())) {
      throw new ForbiddenOperationException("Вы не являетесь автором этого квеста");
    }
  }

  private Team getCurrentUserTeam(User user) {
    return teamMemberRepository
        .findByUser(user)
        .orElseThrow(() -> new ResourceNotFoundException("Пользователь не состоит в команде"))
        .getTeam();
  }

  private QuestRegisterResponse buildQuestRegisterResponse(QuestRegistration registration) {
    return QuestRegisterResponse.builder()
        .questId(registration.getQuest().getId())
        .teamId(registration.getTeam().getId())
        .teamName(registration.getTeam().getName())
        .status(registration.getStatus())
        .build();
  }
}
