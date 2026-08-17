package dn.questenginev2.quest.service;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.TeamNotFoundException;
import dn.questenginev2.level.service.LevelProgressService;
import dn.questenginev2.quest.dto.QuestProgressResponse;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import dn.questenginev2.quest.entity.QuestRegistration;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.repository.QuestRegistrationRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class QuestProgressServiceImpl implements QuestProgressService {

  private final QuestProgressRepository questProgressRepository;
  private final QuestRepository questRepository;
  private final QuestRegistrationRepository questRegistrationRepository;
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final QuestAuthorRepository questAuthorRepository;
  private final UserService userService;
  private final LevelProgressService levelProgressService;
  private final Clock clock;

  @Autowired
  public QuestProgressServiceImpl(
      QuestProgressRepository questProgressRepository,
      QuestRepository questRepository,
      QuestRegistrationRepository questRegistrationRepository,
      TeamRepository teamRepository,
      TeamMemberRepository teamMemberRepository,
      QuestAuthorRepository questAuthorRepository,
      UserService userService,
      LevelProgressService levelProgressService) {
    this(
        questProgressRepository,
        questRepository,
        questRegistrationRepository,
        teamRepository,
        teamMemberRepository,
        questAuthorRepository,
        userService,
        levelProgressService,
        Clock.systemUTC());
  }

  public QuestProgressServiceImpl(
      QuestProgressRepository questProgressRepository,
      QuestRepository questRepository,
      QuestRegistrationRepository questRegistrationRepository,
      TeamRepository teamRepository,
      TeamMemberRepository teamMemberRepository,
      QuestAuthorRepository questAuthorRepository,
      UserService userService,
      LevelProgressService levelProgressService,
      Clock clock) {
    this.questProgressRepository = questProgressRepository;
    this.questRepository = questRepository;
    this.questRegistrationRepository = questRegistrationRepository;
    this.teamRepository = teamRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.questAuthorRepository = questAuthorRepository;
    this.userService = userService;
    this.levelProgressService = levelProgressService;
    this.clock = clock;
  }

  // ────── IMPLEMENTATIONS ───────────────────────────────────────────────────────────
  @Override
  public QuestProgressResponse createProgress(Long questId, Long teamId) {
    Quest quest = validateQuestExist(questId);
    validateQuestRunning(quest);

    Team team = validateTeamExist(teamId);
    validateApprovedRegistration(questId, teamId);
    validateNoDuplicateProgress(questId, teamId);

    QuestProgress progress =
        QuestProgress.builder()
            .quest(quest)
            .team(team)
            .status(QuestProgressStatus.WAITING)
            .questStartedAt(quest.getStartTime())
            .build();

    QuestProgress savedProgress = questProgressRepository.save(progress);
    return buildQuestProgressResponse(savedProgress);
  }

  @Override
  @Transactional
  public QuestProgressResponse enterQuest(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);

    Team team = getCurrentUserTeam(currentUser);
    QuestProgress progress =
        questProgressRepository
            .findByQuestIdAndTeamId(questId, team.getId())
            .orElseThrow(() -> new IllegalArgumentException("Прогресс не найден"));

    validateProgressWaiting(progress);

    progress.setStatus(QuestProgressStatus.RUNNING);
    progress.setEnteredAt(clock.instant());
    QuestProgress savedProgress = questProgressRepository.save(progress);

    levelProgressService.createFirstLevelProgress(savedProgress.getId());

    return buildQuestProgressResponse(savedProgress);
  }

  @Override
  public QuestProgressResponse getProgress(Long questId, Long teamId) {
    QuestProgress progress =
        questProgressRepository
            .findByQuestIdAndTeamId(questId, teamId)
            .orElseThrow(() -> new IllegalArgumentException("Прогресс не найден"));

    return buildQuestProgressResponse(progress);
  }

  @Override
  public List<QuestProgressResponse> getAllByQuest(Long questId) {
    validateQuestExist(questId);

    return questProgressRepository.findByQuestId(questId).stream()
        .map(this::buildQuestProgressResponse)
        .collect(Collectors.toList());
  }

  @Override
  public QuestProgressResponse finishProgress(Long questId, Long teamId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    validateQuestAuthor(currentUser, questId);

    QuestProgress progress =
        questProgressRepository
            .findByQuestIdAndTeamIdAndStatus(questId, teamId, QuestProgressStatus.RUNNING)
            .orElseThrow(() -> new IllegalArgumentException("Прогресс не найден"));

    validateProgressRunning(progress);

    progress.setStatus(QuestProgressStatus.FINISHED);
    progress.setFinishedAt(clock.instant());

    QuestProgress savedProgress = questProgressRepository.save(progress);
    return buildQuestProgressResponse(savedProgress);
  }

  @Override
  public QuestProgressResponse setDnf(Long questId, Long teamId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    validateQuestAuthor(currentUser, questId);

    QuestProgress progress =
        questProgressRepository
            .findByQuestIdAndTeamId(questId, teamId)
            .orElseThrow(() -> new IllegalArgumentException("Прогресс не найден"));

    validateProgressNotFinished(progress);

    progress.setStatus(QuestProgressStatus.DNF);
    progress.setFinishedAt(clock.instant());

    QuestProgress savedProgress = questProgressRepository.save(progress);
    return buildQuestProgressResponse(savedProgress);
  }

  // ────── VALIDATIONS ───────────────────────────────────────────────────────────
  private Quest validateQuestExist(Long questId) {
    return questRepository
        .findById(questId)
        .orElseThrow(() -> new IllegalArgumentException("Квест не найден: " + questId));
  }

  private void validateQuestRunning(Quest quest) {
    if (quest.getStatus() != QuestStatus.RUNNING) {
      throw new ForbiddenOperationException("Создать прогресс можно только для RUNNING квеста");
    }
  }

  private Team validateTeamExist(Long teamId) {
    return teamRepository
        .findById(teamId)
        .orElseThrow(() -> new TeamNotFoundException("Команда не найдена: " + teamId));
  }

  private void validateApprovedRegistration(Long questId, Long teamId) {
    QuestRegistration registration =
        questRegistrationRepository
            .findByQuestIdAndTeamIdAndStatus(
                questId, teamId, dn.questenginev2.quest.entity.RegistrationStatus.APPROVED)
            .orElseThrow(
                () -> new ForbiddenOperationException("Команда не подтверждена для этого квеста"));
  }

  private void validateNoDuplicateProgress(Long questId, Long teamId) {
    if (questProgressRepository.existsByQuestIdAndTeamId(questId, teamId)) {
      throw new IllegalArgumentException("Прогресс для этой команды уже существует");
    }
  }

  private void validateProgressWaiting(QuestProgress progress) {
    if (progress.getStatus() != QuestProgressStatus.WAITING) {
      throw new ForbiddenOperationException("Войти в квест можно только из статуса WAITING");
    }
  }

  private void validateProgressRunning(QuestProgress progress) {
    if (progress.getStatus() != QuestProgressStatus.RUNNING) {
      throw new ForbiddenOperationException("Завершить можно только RUNNING прогресс");
    }
  }

  private void validateProgressNotFinished(QuestProgress progress) {
    if (progress.getStatus() == QuestProgressStatus.FINISHED
        || progress.getStatus() == QuestProgressStatus.DNF) {
      throw new ForbiddenOperationException("Нельзя изменить завершённый прогресс");
    }
  }

  private void validateQuestAuthor(User user, Long questId) {
    if (user.getRole() != UserRole.ADMIN
        && !questAuthorRepository.existsByQuestIdAndUserId(questId, user.getId())) {
      throw new ForbiddenOperationException("Действие доступно только Автору квеста");
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
  private QuestProgressResponse buildQuestProgressResponse(QuestProgress progress) {
    return QuestProgressResponse.builder()
        .teamName(progress.getTeam().getName())
        .status(progress.getStatus())
        .questStartedAt(progress.getQuestStartedAt())
        .endedAt(progress.getEnteredAt())
        .finishedAt(progress.getFinishedAt())
        .build();
  }
}
