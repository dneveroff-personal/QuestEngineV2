package dn.questenginev2.code.service;

import dn.questenginev2.code.dto.CodeResponse;
import dn.questenginev2.code.dto.CreateCodeRequest;
import dn.questenginev2.code.entity.Code;
import dn.questenginev2.code.entity.CodeType;
import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.common.exceptions.ResourceNotFoundException;
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
public class CodeServiceImpl implements CodeService {

  private final CodeRepository codeRepository;
  private final LevelRepository levelRepository;
  private final QuestService questService;
  private final UserService userService;

  // ────── IMPLEMENTATIONS ───────────────────────────────────────────────────────────
  @Override
  public CodeResponse createCode(Long levelId, CreateCodeRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    questService.validateAuthorOrAdmin(currentUser);
    Level level = validateLevelExist(levelId);
    questService.validateQuestAuthor(currentUser, level.getQuest().getId());

    validateCodeValueUnique(levelId, request.value());
    Integer resolvedIndex = resolveCodeIndex(levelId, request);
    validateCodeData(request.type(), resolvedIndex);

    Code code = buildCode(request, level, resolvedIndex);
    Code savedCode = codeRepository.save(code);

    return buildCodeResponse(savedCode, levelId);
  }

  @Override
  public List<CodeResponse> getCodesByLevelId(Long levelId) {
    validateLevelExist(levelId);
    return codeRepository.findByLevelIdOrderByCreatedAt(levelId).stream()
        .map(code -> buildCodeResponse(code, levelId))
        .toList();
  }

  @Override
  public CodeResponse getCodeById(Long codeId) {
    Code code = validateCodeExist(codeId);
    return buildCodeResponse(code);
  }

  @Override
  public CodeResponse updateCode(Long codeId, CreateCodeRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    questService.validateAuthorOrAdmin(currentUser);
    Code code = validateCodeExist(codeId);
    questService.validateQuestAuthor(currentUser, code.getLevel().getQuest().getId());

    if (!code.getValue().equals(request.value())) {
      validateCodeValueUnique(code.getLevel().getId(), request.value());
    }

    Integer resolvedIndex = resolveCodeIndex(code.getLevel().getId(), request);
    validateCodeData(request.type(), resolvedIndex);

    code.setValue(request.value());
    code.setType(request.type());
    code.setBonusPenaltySeconds(request.bonusPenaltySeconds());
    code.setCodeIndex(resolvedIndex);

    Code savedCode = codeRepository.save(code);
    return buildCodeResponse(savedCode);
  }

  @Override
  public void deleteCode(Long codeId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    questService.validateAuthorOrAdmin(currentUser);
    Code code = validateCodeExist(codeId);
    questService.validateQuestAuthor(currentUser, code.getLevel().getQuest().getId());

    codeRepository.delete(code);
  }

  // ────── VALIDATIONS ───────────────────────────────────────────────────────────
  private Level validateLevelExist(Long levelId) {
    return levelRepository
        .findById(levelId)
        .orElseThrow(() -> new ResourceNotFoundException("Уровень не найден: " + levelId));
  }

  private Code validateCodeExist(Long codeId) {
    return codeRepository
        .findById(codeId)
        .orElseThrow(() -> new ResourceNotFoundException("Код не найден: " + codeId));
  }

  private void validateCodeValueUnique(Long levelId, String codeValue) {
    if (codeRepository.existsByLevelIdAndValue(levelId, codeValue)) {
      throw new IllegalArgumentException("Код уже существует: " + codeValue);
    }
  }

  /**
   * MAIN: codeIndex обязателен для учёта distinct MAIN (ADR-0005). Если клиент не
   * передал — назначаем max(existing)+1 на уровне (форма автора не требует поля).
   * BONUS/PENALTY: codeIndex запрещён.
   */
  private Integer resolveCodeIndex(Long levelId, CreateCodeRequest request) {
    if (request.type() != CodeType.MAIN) {
      return null;
    }
    if (request.codeIndex() != null) {
      return request.codeIndex();
    }
    return codeRepository.maxMainCodeIndexByLevelId(levelId) + 1;
  }

  private void validateCodeData(CodeType type, Integer codeIndex) {
    if (type == CodeType.MAIN && codeIndex == null) {
      throw new IllegalArgumentException("Для MAIN-кода необходимо указать codeIndex");
    }

    if (type != CodeType.MAIN && codeIndex != null) {
      throw new IllegalArgumentException("codeIndex допускается только для MAIN-кода");
    }
  }

  // ────── BUILDERS ───────────────────────────────────────────────────────────
  private CodeResponse buildCodeResponse(Code code) {
    return CodeResponse.builder()
        .id(code.getId())
        .levelId(code.getLevel().getId())
        .value(code.getValue())
        .type(code.getType())
        .bonusPenaltySeconds(code.getBonusPenaltySeconds())
        .codeIndex(code.getCodeIndex())
        .createdAt(code.getCreatedAt())
        .build();
  }

  private CodeResponse buildCodeResponse(Code code, Long levelId) {
    return CodeResponse.builder()
        .id(code.getId())
        .levelId(levelId)
        .value(code.getValue())
        .type(code.getType())
        .bonusPenaltySeconds(code.getBonusPenaltySeconds())
        .codeIndex(code.getCodeIndex())
        .createdAt(code.getCreatedAt())
        .build();
  }

  private Code buildCode(CreateCodeRequest request, Level level, Integer codeIndex) {
    return Code.builder()
        .level(level)
        .value(request.value())
        .type(request.type())
        .bonusPenaltySeconds(request.bonusPenaltySeconds())
        .codeIndex(codeIndex)
        .createdAt(Instant.now())
        .build();
  }
}
