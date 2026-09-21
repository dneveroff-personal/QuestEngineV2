package dn.questenginev2.quest.service;

import dn.questenginev2.bonuspenalty.service.BonusPenaltyService;
import dn.questenginev2.common.exceptions.ConflictException;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.LevelProgressNotFoundException;
import dn.questenginev2.common.exceptions.ResourceNotFoundException;
import dn.questenginev2.common.exceptions.TeamNotFoundException;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.level.repository.LevelRepository;
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
import dn.questenginev2.statistic.event.StatisticsChangedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
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
  private final BonusPenaltyService bonusPenaltyService;
  private final Clock clock;
  private ApplicationEventPublisher eventPublisher;
  private LevelProgressRepository levelProgressRepository;
  private LevelRepository levelRepository;

  @Autowired
  public QuestProgressServiceImpl(
      QuestProgressRepository questProgressRepository,
      QuestRepository questRepository,
      QuestRegistrationRepository questRegistrationRepository,
      TeamRepository teamRepository,
      TeamMemberRepository teamMemberRepository,
      QuestAuthorRepository questAuthorRepository,
      UserService userService,
      LevelProgressService levelProgressService,
      BonusPenaltyService bonusPenaltyService,
      LevelProgressRepository levelProgressRepository,
      LevelRepository levelRepository) {
    this(
        questProgressRepository,
        questRepository,
        questRegistrationRepository,
        teamRepository,
        teamMemberRepository,
        questAuthorRepository,
        userService,
        levelProgressService,
        bonusPenaltyService,
        Clock.systemUTC());
    this.levelProgressRepository = levelProgressRepository;
    this.levelRepository = levelRepository;
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
      BonusPenaltyService bonusPenaltyService,
      Clock clock) {
    this.questProgressRepository = questProgressRepository;
    this.questRepository = questRepository;
    this.questRegistrationRepository = questRegistrationRepository;
    this.teamRepository = teamRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.questAuthorRepository = questAuthorRepository;
    this.userService = userService;
    this.levelProgressService = levelProgressService;
    this.bonusPenaltyService = bonusPenaltyService;
    this.clock = clock;
  }

  @Autowired
  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  private void notifyStatistics(Long questId) {
    if (eventPublisher != null && questId != null) {
      eventPublisher.publishEvent(new StatisticsChangedEvent(questId));
    }
  }

  @Override
  public QuestProgressResponse createProgress(Long questId, Long teamId) {
    Quest quest = validateQuestExist(questId);
    validateQuestRunning(quest);
    Team team = validateTeamExist(teamId);
    validateApprovedRegistration(questId, teamId);

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
  public QuestProgressResponse enterQuest(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Team team = getCurrentUserTeam(currentUser);
    QuestProgress progress =
        questProgressRepository
            .findByQuestIdAndTeamId(questId, team.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Прогресс не найден"));

    validateProgressWaiting(progress);

    progress.setStatus(QuestProgressStatus.RUNNING);
    progress.setEnteredAt(clock.instant());
    QuestProgress savedProgress = questProgressRepository.save(progress);
    levelProgressService.createFirstLevelProgress(savedProgress);
    notifyStatistics(questId);
    return buildQuestProgressResponse(savedProgress);
  }

  @Override
  public QuestProgressResponse getProgress(Long questId, Long teamId) {
    QuestProgress progress =
        questProgressRepository
            .findByQuestIdAndTeamId(questId, teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Прогресс не найден"));
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
            .orElseThrow(() -> new ResourceNotFoundException("Прогресс не найден"));

    validateProgressRunning(progress);

    progress.setStatus(QuestProgressStatus.FINISHED);
    progress.setFinishedAt(clock.instant());
    QuestProgress savedProgress = questProgressRepository.save(progress);
    notifyStatistics(questId);
    return buildQuestProgressResponse(savedProgress);
  }

  @Override
  public QuestProgressResponse setDnf(Long questId, Long teamId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    validateQuestAuthor(currentUser, questId);

    Quest quest = validateQuestExist(questId);
    validateQuestFinished(quest);

    QuestProgress progress =
        questProgressRepository
            .findByQuestIdAndTeamId(questId, teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Прогресс не найден"));

    progress.setStatus(QuestProgressStatus.DNF);
    progress.setFinishedAt(clock.instant());
    QuestProgress savedProgress = questProgressRepository.save(progress);
    notifyStatistics(questId);
    return buildQuestProgressResponse(savedProgress);
  }

  @Override
  public QuestProgressResponse completeLevel(Long levelProgressId, Authentication auth) {
    LevelProgress levelProgress =
        levelProgressRepository
            .findById(levelProgressId)
            .orElseThrow(() -> new LevelProgressNotFoundException("Текущий Level Progress найден"));

    QuestProgress questProgress = levelProgress.getQuestProgress();
    validateTeamProgress(questProgress, auth);
    levelProgressService.completeLevel(levelProgressId);
    return advanceAfterLevelCompleted(levelProgress);
  }

  @Override
  public QuestProgressResponse advanceAfterLevelCompleted(LevelProgress completedLevelProgress) {
    QuestProgress questProgress = completedLevelProgress.getQuestProgress();
    Long questId = questProgress.getQuest().getId();

    int nextLevelOrderIdx = completedLevelProgress.getLevel().getOrderIndex() + 1;
    Level nextLevel =
        levelRepository.findByQuestIdAndOrderIndex(questId, nextLevelOrderIdx).orElse(null);

    if (nextLevel != null) {
      levelProgressService.createNextLevelProgress(questProgress, nextLevelOrderIdx);
      notifyStatistics(questId);
      return buildQuestProgressResponse(questProgress);
    }

    questProgress.setStatus(QuestProgressStatus.FINISHED);
    questProgress.setFinishedAt(clock.instant());
    QuestProgress savedQuestProgress = questProgressRepository.save(questProgress);
    notifyStatistics(questId);
    return buildQuestProgressResponse(savedQuestProgress);
  }

  private Quest validateQuestExist(Long questId) {
    return questRepository
        .findById(questId)
        .orElseThrow(() -> new ResourceNotFoundException("Квест не найден: " + questId));
  }

  private void validateQuestRunning(Quest quest) {
    if (quest.getStatus() != QuestStatus.RUNNING) {
      throw new ConflictException("Создать прогресс можно только для RUNNING квеста");
    }
  }

  private void validateQuestFinished(Quest quest) {
    if (quest.getStatus() != QuestStatus.FINISHED) {
      throw new ConflictException(
          "Ручной DNF доступен только после завершения Quest автором (Quest.status = FINISHED)");
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
            .findByQuestIdAndTeamId(questId, teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Регистрация не найдена"));
    if (registration.getStatus() != dn.questenginev2.quest.entity.RegistrationStatus.APPROVED) {
      throw new ConflictException("Команда должна быть APPROVED для создания прогресса");
    }
  }

  private void validateProgressWaiting(QuestProgress progress) {
    if (progress.getStatus() != QuestProgressStatus.WAITING) {
      throw new ConflictException("Войти можно только из статуса WAITING");
    }
  }

  private void validateProgressRunning(QuestProgress progress) {
    if (progress.getStatus() != QuestProgressStatus.RUNNING) {
      throw new ConflictException("Операция доступна только для RUNNING прогресса");
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

  private void validateTeamProgress(QuestProgress progress, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Team team = getCurrentUserTeam(currentUser);
    if (!team.getId().equals(progress.getTeam().getId())) {
      throw new ForbiddenOperationException(
          "Попытка пользователя управлять не своим QuestProgress");
    }
  }

  private QuestProgressResponse buildQuestProgressResponse(QuestProgress progress) {
    return QuestProgressResponse.builder()
        .id(progress.getId())
        .teamName(progress.getTeam().getName())
        .status(progress.getStatus())
        .questStartedAt(progress.getQuestStartedAt())
        .endedAt(progress.getEnteredAt())
        .finishedAt(progress.getFinishedAt())
        .bonusPenaltySeconds(bonusPenaltyService.getTotalAdjustmentSeconds(progress))
        .build();
  }
}
