package dn.questenginev2.hint.service;

import dn.questenginev2.hint.dto.CreateHintRequest;
import dn.questenginev2.hint.dto.HintResponse;
import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.hint.entity.HintType;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.repository.LevelRepository;
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
public class HintServiceImpl implements HintService {

  private final HintRepository hintRepository;
  private final LevelRepository levelRepository;
  private final QuestService questService;
  private final UserService userService;

  // ────── IMPLEMENTATIONS ───────────────────────────────────────────────────────────
  @Override
  public HintResponse createHint(Long levelId, CreateHintRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    questService.validateAuthorOrAdmin(currentUser);
    Level level = validateLevelExist(levelId);
    questService.validateQuestAuthor(currentUser, level.getQuest().getId());

    validateHintData(request);
    Hint hint = buildHint(request, level);
    Hint savedHint = hintRepository.save(hint);

    return buildHintResponse(savedHint, levelId);
  }

  @Override
  public List<HintResponse> getHintsByLevelId(Long levelId) {
    return hintRepository.findByLevelIdOrderByOrderIndex(levelId).stream()
        .map(hint -> buildHintResponse(hint, levelId))
        .toList();
  }

  @Override
  public HintResponse getHintById(Long hintId) {
    Hint hint = validateHintExist(hintId);
    return buildHintResponse(hint);
  }

  @Override
  public HintResponse updateHint(Long hintId, CreateHintRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    questService.validateAuthorOrAdmin(currentUser);
    Hint hint = validateHintExist(hintId);
    questService.validateQuestAuthor(currentUser, hint.getLevel().getQuest().getId());

    hint.setOrderIndex(request.orderIndex());
    hint.setDelaySeconds(request.delaySeconds());
    hint.setContent(request.content());
    hint.setUpdatedAt(Instant.now());

    validateHintData(request);
    hint.setType(request.type());
    hint.setBonusPenaltySeconds(request.bonusPenaltySeconds());

    Hint savedHint = hintRepository.save(hint);
    return buildHintResponse(savedHint);
  }

  @Override
  public void deleteHint(Long hintId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    questService.validateAuthorOrAdmin(currentUser);
    Hint hint = validateHintExist(hintId);
    questService.validateQuestAuthor(currentUser, hint.getLevel().getQuest().getId());

    hintRepository.delete(hint);
  }

  @Override
  public Integer getMaxHintIndex(Long levelId) {
    Integer maxIndex = hintRepository.findMaxOrderIndex(levelId);
    return maxIndex != null ? maxIndex : 0;
  }

  // ────── VALIDATIONS ───────────────────────────────────────────────────────────
  private Level validateLevelExist(Long levelId) {
    return levelRepository
        .findById(levelId)
        .orElseThrow(() -> new IllegalArgumentException("Уровень не найден: " + levelId));
  }

  private Hint validateHintExist(Long hintId) {
    return hintRepository
        .findById(hintId)
        .orElseThrow(() -> new IllegalArgumentException("Подсказка не найдена: " + hintId));
  }

  /**
   * ADR-0020: bonusPenaltySeconds обязателен для BONUS/PENALTY, недопустим для REGULAR
   * (симметрично validateCodeData в CodeServiceImpl для MAIN-кодов).
   */
  private void validateHintData(CreateHintRequest request) {
    if (request.type() != HintType.REGULAR && request.bonusPenaltySeconds() == null) {
      throw new IllegalArgumentException(
          "Для подсказки типа " + request.type() + " необходимо указать bonusPenaltySeconds");
    }

    if (request.type() == HintType.REGULAR && request.bonusPenaltySeconds() != null) {
      throw new IllegalArgumentException(
          "bonusPenaltySeconds недопустим для подсказки типа REGULAR");
    }
  }

  // ────── BUILDERS ───────────────────────────────────────────────────────────
  private HintResponse buildHintResponse(Hint hint) {
    return HintResponse.builder()
        .id(hint.getId())
        .levelId(hint.getLevel().getId())
        .orderIndex(hint.getOrderIndex())
        .delaySeconds(hint.getDelaySeconds())
        .content(hint.getContent())
        .type(hint.getType())
        .bonusPenaltySeconds(hint.getBonusPenaltySeconds())
        .createdAt(hint.getCreatedAt())
        .updatedAt(hint.getUpdatedAt())
        .build();
  }

  private HintResponse buildHintResponse(Hint hint, Long levelId) {
    return HintResponse.builder()
        .id(hint.getId())
        .levelId(levelId)
        .orderIndex(hint.getOrderIndex())
        .delaySeconds(hint.getDelaySeconds())
        .content(hint.getContent())
        .type(hint.getType())
        .bonusPenaltySeconds(hint.getBonusPenaltySeconds())
        .createdAt(hint.getCreatedAt())
        .updatedAt(hint.getUpdatedAt())
        .build();
  }

  private Hint buildHint(CreateHintRequest request, Level level) {
    return Hint.builder()
        .level(level)
        .orderIndex(request.orderIndex())
        .delaySeconds(request.delaySeconds())
        .content(request.content())
        .type(request.type())
        .bonusPenaltySeconds(request.bonusPenaltySeconds())
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }
}
