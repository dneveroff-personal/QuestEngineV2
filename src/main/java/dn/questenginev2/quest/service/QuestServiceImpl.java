package dn.questenginev2.quest.service;

import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
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

  // ────── IMPLEMENTATIONS ───────────────────────────────────────────────────────────
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
    quest.setDescription(request.description());
    quest.setType(request.type());
    quest.setStartTime(request.startTime());
    quest.setFinishTime(request.finishTime());

    Quest savedQuest = questRepository.save(quest);
    return buildQuestResponse(savedQuest);
  }

  @Override
  public QuestResponse publishQuest(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Quest quest = validateQuestExist(questId);
    validateQuestAuthor(currentUser, questId);
    validateQuestStatus(quest, QuestStatus.DRAFT, "опубликовать");
    validateQuestPublishable(quest);

    quest.setStatus(QuestStatus.REGISTRATION);
    Quest savedQuest = questRepository.save(quest);
    return buildQuestResponse(savedQuest);
  }

  @Override
  public QuestResponse finishQuest(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Quest quest = validateQuestExist(questId);
    validateQuestAuthor(currentUser, questId);
    validateQuestStatus(quest, QuestStatus.RUNNING, "завершить");

    quest.setStatus(QuestStatus.FINISHED);
    Quest savedQuest = questRepository.save(quest);
    markUnfinishedProgressesAsDnf(questId);
    return buildQuestResponse(savedQuest);
  }

  @Override
  public void delete(Long questId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    Quest quest = validateQuestExist(questId);
    validateQuestAuthor(currentUser, questId);

    questAuthorRepository.deleteByQuestId(questId);
    questRepository.delete(quest);
  }

  @Override
  public List<QuestShortProjection> getAllUpcomingBrief() {
    return questRepository.findAllByStartTimeAfter(Instant.now());
  }

  // ────── VALIDATIONS ───────────────────────────────────────────────────────────
  @Override
  public Quest validateQuestExist(Long questId) {
    return questRepository
        .findById(questId)
        .orElseThrow(() -> new IllegalArgumentException("Квест не найден: " + questId));
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
    if (user.getRole() != UserRole.ADMIN
        && !questAuthorRepository.existsByQuestIdAndUserId(questId, user.getId())) {
      throw new ForbiddenOperationException("Редактировать квесты могут только Авторы");
    }
  }

  private void validateQuestStatus(Quest quest, QuestStatus required, String action) {
    if (quest.getStatus() != required) {
      throw new ForbiddenOperationException(
          "Действие \""
              + action
              + "\" доступно только для квеста в статусе "
              + required
              + ", текущий статус: "
              + quest.getStatus());
    }
  }

  /**
   * Проверка содержимого квеста перед публикацией: должен быть хотя бы один Level, и ни один
   * Level не должен быть "аномальным" (без кодов и без автоперехода) — см. ADR-0005.
   */
  private void validateQuestPublishable(Quest quest) {
    List<Level> levels = levelRepository.findByQuestIdOrderByOrderIndex(quest.getId());
    if (levels.isEmpty()) {
      throw new ForbiddenOperationException("Нельзя опубликовать квест без уровней");
    }
    for (Level level : levels) {
      boolean hasAutoTransition = level.getTimeoutSeconds() != null;
      boolean hasCodes = codeRepository.existsByLevelId(level.getId());
      if (!hasAutoTransition && !hasCodes) {
        throw new ForbiddenOperationException(
            "Уровень \""
                + level.getTitle()
                + "\" (id="
                + level.getId()
                + ") непроходим: нет ни кодов, ни автоперехода (ADR-0005, \"аномальный\""
                + " уровень)");
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

  // ────── BUILDERS ───────────────────────────────────────────────────────────
  private QuestResponse buildQuestResponse(Quest quest) {
    return QuestResponse.builder()
        .id(quest.getId())
        .title(quest.getTitle())
        .description(quest.getDescription())
        .type(quest.getType())
        .status(quest.getStatus())
        .createdAt(quest.getCreatedAt())
        .startTime(quest.getStartTime())
        .finishTime(quest.getFinishTime())
        .build();
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
