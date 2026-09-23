package dn.questenginev2.level.service;

import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.common.exceptions.ConflictException;
import dn.questenginev2.common.exceptions.ResourceNotFoundException;
import dn.questenginev2.level.dto.CreateLevelRequest;
import dn.questenginev2.level.dto.LevelResponse;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.service.QuestService;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Transactional
@AllArgsConstructor
public class LevelServiceImpl implements LevelService {

  private final LevelRepository levelRepository;
  private final CodeRepository codeRepository;
  private final QuestService questService;
  private final UserService userService;

  @Override
  public LevelResponse createLevel(Long questId, CreateLevelRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    questService.validateAuthorOrAdmin(currentUser);
    Quest quest = questService.validateQuestExist(questId);
    questService.validateQuestAuthor(currentUser, questId);

    Level level = buildLevel(request, quest);
    Level savedLevel = levelRepository.save(level);

    return buildLevelResponse(savedLevel);
  }

  @Override
  public List<LevelResponse> getLevelsByQuestId(Long questId) {
    return levelRepository.findByQuestIdOrderByOrderIndex(questId).stream()
        .map(this::buildLevelResponse)
        .toList();
  }

  @Override
  public LevelResponse getLevelById(Long levelId) {
    Level level = validateLevelExist(levelId);
    return buildLevelResponse(level);
  }

  @Override
  public LevelResponse updateLevel(Long levelId, CreateLevelRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    questService.validateAuthorOrAdmin(currentUser);
    Level level = validateLevelExist(levelId);
    questService.validateQuestAuthor(currentUser, level.getQuest().getId());

    level.setTitle(request.title());
    level.setContent(request.content());
    level.setTimeoutSeconds(request.timeoutSeconds());
    level.setUpdatedAt(Instant.now());
    level.setRequiredMainCodesCount(request.requiredMainCodesCount());
    validateRequiredMainCodesCount(level);

    Level savedLevel = levelRepository.save(level);
    return buildLevelResponse(savedLevel);
  }

  @Override
  public void deleteLevel(Long levelId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    questService.validateAuthorOrAdmin(currentUser);
    Level level = validateLevelExist(levelId);
    questService.validateQuestAuthor(currentUser, level.getQuest().getId());

    levelRepository.delete(level);
  }

  @Override
  public Integer getMaxLevelIndex(Long questId) {
    Integer maxIndex = levelRepository.findMaxOrderIndex(questId);
    return maxIndex != null ? maxIndex : 0;
  }

  private Level validateLevelExist(Long levelId) {
    return levelRepository
        .findById(levelId)
        .orElseThrow(() -> new ResourceNotFoundException("Уровень не найден: " + levelId));
  }

  /**
   * requiredMainCodesCount не может превышать число различных MAIN-кодов уровня (ADR-0005).
   * При создании уровня кодов ещё нет — проверка срабатывает на update и на publish.
   */
  private void validateRequiredMainCodesCount(Level level) {
    Integer required = level.getRequiredMainCodesCount();
    if (required == null) {
      return;
    }
    long mainCount = codeRepository.countDistinctMainCodeIndexesByLevelId(level.getId());
    if (required > mainCount) {
      throw new ConflictException(
          "requiredMainCodesCount="
              + required
              + " больше числа MAIN-кодов уровня ("
              + mainCount
              + ") — порог недостижим (ADR-0005)");
    }
  }

  private LevelResponse buildLevelResponse(Level level) {
    return LevelResponse.builder()
        .id(level.getId())
        .questId(level.getQuest().getId())
        .title(level.getTitle())
        .orderIndex(level.getOrderIndex())
        .content(level.getContent())
        .requiredMainCodesCount(level.getRequiredMainCodesCount())
        .timeoutSeconds(level.getTimeoutSeconds())
        .createdAt(level.getCreatedAt())
        .updatedAt(level.getUpdatedAt())
        .build();
  }

  private Level buildLevel(CreateLevelRequest request, Quest quest) {
    return Level.builder()
        .quest(quest)
        .title(request.title())
        .orderIndex(getMaxLevelIndex(quest.getId()) + 1)
        .content(request.content())
        .timeoutSeconds(request.timeoutSeconds())
        .requiredMainCodesCount(request.requiredMainCodesCount())
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }
}
