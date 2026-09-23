package dn.questenginev2.quest.service;

import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.common.exceptions.ConflictException;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.ResourceNotFoundException;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.dto.CreateQuestRequest;
import dn.questenginev2.quest.dto.QuestResponse;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestAuthor;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.statistic.event.StatisticsChangedEvent;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestServiceImpl implements QuestService {

  private final QuestRepository questRepository;
  private final QuestAuthorRepository questAuthorRepository;
  private final LevelRepository levelRepository;
  private final CodeRepository codeRepository;
  private final QuestProgressRepository questProgressRepository;
  private final UserService userService;
  private final ApplicationEventPublisher eventPublisher;

  private void publishStatisticsChanged(Long questId) {
    eventPublisher.publishEvent(new StatisticsChangedEvent(questId));
  }

  @Override
  public QuestResponse createQuest(CreateQuestRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    validateAuthorOrAdmin(currentUser);
    Quest quest = buildQuest(request);
    Quest savedQuest = questRepository.save(quest);
    questAuthorRepository.save(buildQuestAuthor(savedQuest, currentUser));
    return buildQuestResponse(savedQuest);
  }

  @Override
  public QuestResponse getQuestById(Long questId) {
    Quest quest = validateQuestExist(questId);
    return buildQuestResponse(quest);
  }

  @Override
  public List<QuestResponse> getQuestsByAuthor(Long authorId) {
    return questAuthorRepository.findByUserId(authorId).stream()
        .map(qa -> buildQuestResponse(qa.getQuest()))
        .toList();
  }

  @Override
  public QuestResponse updateQuest(Long questId, CreateQuestRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Quest quest = validateQuestExist(questId);
    validateQuestAuthor(currentUser, questId);
    validateQuestNotArchived(quest);

    quest.setTitle(request.title());
    if (request.description() != null) {
      quest.setDescription(request.description());
    }
    quest.setType(request.type());
    quest.setStartTime(request.startTime());
    quest.setFinishTime(request.finishTime());
    if (request.maximumTeams() != null) {
      quest.setMaximumTeams(request.maximumTeams());
    }

    return buildQuestResponse(questRepository.save(quest));
  }

  @Override
  public QuestResponse publishQuest(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Quest quest = validateQuestExist(questId);
    validateQuestAuthor(currentUser, questId);
    validateQuestNotArchived(quest);
    validateQuestStatus(quest, QuestStatus.DRAFT, "опубликовать");
    validateQuestPublishable(quest);

    quest.setStatus(QuestStatus.REGISTRATION);
    return buildQuestResponse(questRepository.save(quest));
  }

  @Override
  public QuestResponse finishQuest(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Quest quest = validateQuestExist(questId);
    validateQuestAuthor(currentUser, questId);
    validateQuestNotArchived(quest);
    validateQuestStatus(quest, QuestStatus.RUNNING, "завершить");

    quest.setStatus(QuestStatus.FINISHED);
    quest.setFinishTime(Instant.now());
    markUnfinishedProgressesAsDnf(questId);
    QuestResponse response = buildQuestResponse(questRepository.save(quest));
    publishStatisticsChanged(questId);
    return response;
  }

  @Override
  public QuestResponse archiveQuest(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Quest quest = validateQuestExist(questId);
    validateQuestAuthor(currentUser, questId);
    quest.setArchived(true);
    return buildQuestResponse(questRepository.save(quest));
  }

  @Override
  public Quest validateQuestExist(Long questId) {
    return questRepository
        .findById(questId)
        .orElseThrow(() -> new ResourceNotFoundException("Квест не найден: " + questId));
  }

  @Override
  public void validateAuthorOrAdmin(User user) {
    if (user.getRole() != UserRole.AUTHOR && user.getRole() != UserRole.ADMIN) {
      throw new ForbiddenOperationException(
          "Доступ к редактированию квестов имеют только AUTHOR или ADMIN");
    }
  }

  @Override
  public void validateQuestAuthor(User user, Long questId) {
    if (user.getRole() == UserRole.ADMIN) {
      return;
    }
    if (!questAuthorRepository.existsByQuestIdAndUserId(questId, user.getId())) {
      throw new ForbiddenOperationException("Только автор квеста может выполнить это действие");
    }
  }

  private void validateQuestStatus(Quest quest, QuestStatus required, String action) {
    if (quest.getStatus() != required) {
      throw new ConflictException(
          "Нельзя "
              + action
              + " квест в статусе "
              + quest.getStatus()
              + " (требуется "
              + required
              + ")");
    }
  }

  private void validateQuestNotArchived(Quest quest) {
    if (Boolean.TRUE.equals(quest.getArchived())) {
      throw new ConflictException("Квест в архиве, изменение запрещено");
    }
  }

  private void validateQuestPublishable(Quest quest) {
    List<Level> levels = levelRepository.findByQuestIdOrderByOrderIndex(quest.getId());
    if (levels.isEmpty()) {
      throw new ConflictException("Нельзя опубликовать квест без уровней");
    }
    for (Level level : levels) {
      boolean hasAutoTransition = level.getTimeoutSeconds() != null;
      boolean hasCodes = codeRepository.existsByLevelId(level.getId());
      if (!hasAutoTransition && !hasCodes) {
        throw new ConflictException(
            "Уровень \""
                + level.getTitle()
                + "\" (id="
                + level.getId()
                + ") непроходим: нет ни кодов, ни автоперехода (ADR-0005, \"аномальный\""
                + " уровень)");
      }
      Integer required = level.getRequiredMainCodesCount();
      if (required != null) {
        long mainCount = codeRepository.countDistinctMainCodeIndexesByLevelId(level.getId());
        if (required > mainCount) {
          throw new ConflictException(
              "Уровень \""
                  + level.getTitle()
                  + "\" (id="
                  + level.getId()
                  + "): requiredMainCodesCount="
                  + required
                  + " больше числа MAIN-кодов ("
                  + mainCount
                  + ") — порог недостижим (ADR-0005)");
        }
      }
    }
  }

  private void markUnfinishedProgressesAsDnf(Long questId) {
    List<QuestProgress> progresses = questProgressRepository.findByQuestId(questId);
    for (QuestProgress progress : progresses) {
      if (progress.getStatus() != QuestProgressStatus.FINISHED) {
        progress.setStatus(QuestProgressStatus.DNF);
      }
    }
    questProgressRepository.saveAll(progresses);
  }

  private QuestResponse buildQuestResponse(Quest quest) {
    var builder =
        QuestResponse.builder()
            .id(quest.getId())
            .title(quest.getTitle())
            .description(quest.getDescription())
            .type(quest.getType())
            .status(quest.getStatus())
            .createdAt(quest.getCreatedAt())
            .startTime(quest.getStartTime())
            .finishTime(quest.getFinishTime())
            .maximumTeams(quest.getMaximumTeams())
            .archived(quest.getArchived());

    questAuthorRepository
        .findPrimaryAuthor(quest.getId())
        .ifPresent(
            qa -> {
              var user = qa.getUser();
              builder.authorId(user.getId());
              String name =
                  user.getPublicName() != null && !user.getPublicName().isBlank()
                      ? user.getPublicName()
                      : user.getUsername();
              builder.authorName(name);
            });

    return builder.build();
  }

  private Quest buildQuest(CreateQuestRequest request) {
    return Quest.builder()
        .title(request.title())
        .description(request.description() != null ? request.description() : "")
        .type(request.type())
        .status(QuestStatus.DRAFT)
        .createdAt(Instant.now())
        .startTime(request.startTime())
        .finishTime(request.finishTime())
        .maximumTeams(request.maximumTeams() != null ? request.maximumTeams() : 100)
        .build();
  }

  private QuestAuthor buildQuestAuthor(Quest savedQuest, User currentUser) {
    return QuestAuthor.builder().quest(savedQuest).user(currentUser).build();
  }
}
