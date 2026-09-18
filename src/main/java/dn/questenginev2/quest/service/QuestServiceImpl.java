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
import dn.questenginev2.quest.entity.QuestShortProjection;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.repository.QuestRepository;
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
public class QuestServiceImpl implements QuestService {

  private final QuestAuthorRepository questAuthorRepository;
  private final QuestRepository questRepository;
  private final QuestProgressRepository questProgressRepository;
  private final LevelRepository levelRepository;
  private final CodeRepository codeRepository;
  private final UserService userService;

  @Override
  public QuestResponse createQuest(CreateQuestRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    validateAuthorOrAdmin(currentUser);

    Quest quest = buildQuest(request);
    Quest savedQuest = questRepository.save(quest);
    QuestAuthor author = buildQuestAuthor(savedQuest, currentUser);
    questAuthorRepository.save(author);

    return buildQuestResponse(savedQuest);
  }

  @Override
  public QuestResponse getQuestById(Long questId) {
    Quest quest = validateQuestExist(questId);
    return buildQuestResponse(quest);
  }

  @Override
  public List<QuestResponse> getAllByAuthorId(Long authorId) {
    userService.getUser(authorId);
    return questAuthorRepository.findByUserId(authorId).stream()
        .map(QuestAuthor::getQuest)
        .map(this::buildQuestResponse)
        .collect(Collectors.toList());
  }

  @Override
  public QuestResponse updateQuest(Long questId, CreateQuestRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Quest quest = validateQuestExist(questId);
    validateQuestAuthor(currentUser, questId);

    quest.setTitle(request.title());
    if (request.description() != null) {
      quest.setDescription(request.description());
    }
    quest.setType(request.type());
    quest.setStartTime(request.startTime());
    quest.setFinishTime(request.finishTime());

    return buildQuestResponse(questRepository.save(quest));
  }

  @Override
  public QuestResponse publishQuest(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Quest quest = validateQuestExist(questId);
    validateQuestAuthor(currentUser, questId);
    validateLevelsConfigured(questId);

    if (quest.getStatus() != QuestStatus.DRAFT) {
      throw new ConflictException("Quest can only be published from DRAFT status");
    }
    quest.setStatus(QuestStatus.REGISTRATION);
    return buildQuestResponse(questRepository.save(quest));
  }

  @Override
  public QuestResponse finishQuest(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Quest quest = validateQuestExist(questId);
    validateQuestAuthor(currentUser, questId);

    if (quest.getStatus() != QuestStatus.RUNNING) {
      throw new ConflictException("Quest can only be finished from RUNNING status");
    }
    quest.setStatus(QuestStatus.FINISHED);
    quest.setFinishTime(Instant.now());
    markUnfinishedProgressesAsDnf(questId);
    return buildQuestResponse(questRepository.save(quest));
  }

  @Override
  public void delete(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    validateQuestExist(questId);
    validateQuestAuthor(currentUser, questId);
    questAuthorRepository.deleteByQuestId(questId);
    questRepository.deleteById(questId);
  }

  @Override
  public List<QuestShortProjection> getAllUpcomingBrief() {
    return questRepository.findUpcoming(List.of(QuestStatus.REGISTRATION, QuestStatus.RUNNING));
  }

  @Override
  public Quest validateQuestExist(Long questId) {
    return questRepository
        .findById(questId)
        .orElseThrow(() -> new ResourceNotFoundException("Quest not found: " + questId));
  }

  @Override
  public void validateAuthorOrAdmin(User user) {
    if (user.getRole() != UserRole.AUTHOR && user.getRole() != UserRole.ADMIN) {
      throw new ForbiddenOperationException("Only AUTHOR or ADMIN can create quests");
    }
  }

  @Override
  public void validateQuestAuthor(User user, Long questId) {
    if (user.getRole() == UserRole.ADMIN) {
      return;
    }
    if (!questAuthorRepository.existsByQuestIdAndUserId(questId, user.getId())) {
      throw new ForbiddenOperationException("Not an author of this quest");
    }
  }

  private void validateLevelsConfigured(Long questId) {
    List<Level> levels = levelRepository.findByQuestIdOrderByOrderIndexAsc(questId);
    if (levels.isEmpty()) {
      throw new ConflictException("Cannot publish quest without levels");
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
                + ") непроходим: нет ни кодов, ни автоперехода (ADR-0005)");
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
            .finishTime(quest.getFinishTime());

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
        .build();
  }

  private QuestAuthor buildQuestAuthor(Quest savedQuest, User currentUser) {
    return QuestAuthor.builder().quest(savedQuest).user(currentUser).build();
  }
}
