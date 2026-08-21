package dn.questenginev2.code.service;

import dn.questenginev2.code.dto.CodeResponse;
import dn.questenginev2.code.dto.CreateCodeRequest;
import dn.questenginev2.code.entity.Code;
import dn.questenginev2.code.entity.CodeType;
import dn.questenginev2.code.repository.CodeRepository;
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
    validateCodeData(request);

    Code code = buildCode(request, level);
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

    validateCodeData(request);

    code.setValue(request.value());
    code.setType(request.type());
    code.setPoints(request.points());
    code.setGroupIndex(request.groupIndex());

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
        .orElseThrow(() -> new IllegalArgumentException("Уровень не найден: " + levelId));
  }

  private Code validateCodeExist(Long codeId) {
    return codeRepository
        .findById(codeId)
        .orElseThrow(() -> new IllegalArgumentException("Код не найден: " + codeId));
  }

  private void validateCodeValueUnique(Long LevelId, String codeValue) {
    if (codeRepository.existsByLevelIdAndValue(LevelId, codeValue)) {
      throw new IllegalArgumentException("Код уже существует: " + codeValue);
    }
  }

  private void validateCodeData(CreateCodeRequest request) {
    if (request.type() == CodeType.MAIN && request.groupIndex() == null) {
      throw new IllegalArgumentException("Для MAIN-кода необходимо указать groupIndex");
    }

    if (request.type() != CodeType.MAIN && request.groupIndex() != null) {
      throw new IllegalArgumentException("groupIndex допускается только для MAIN-кода");
    }
  }

  // ────── BUILDERS ───────────────────────────────────────────────────────────
  private CodeResponse buildCodeResponse(Code code) {
    return CodeResponse.builder()
        .id(code.getId())
        .levelId(code.getLevel().getId())
        .value(code.getValue())
        .type(code.getType())
        .points(code.getPoints())
        .groupIndex(code.getGroupIndex())
        .createdAt(code.getCreatedAt())
        .build();
  }

  private CodeResponse buildCodeResponse(Code code, Long levelId) {
    return CodeResponse.builder()
        .id(code.getId())
        .levelId(levelId)
        .value(code.getValue())
        .type(code.getType())
        .points(code.getPoints())
        .groupIndex(code.getGroupIndex())
        .createdAt(code.getCreatedAt())
        .build();
  }

  private Code buildCode(CreateCodeRequest request, Level level) {
    return Code.builder()
        .level(level)
        .value(request.value())
        .type(request.type())
        .points(request.points())
        .groupIndex(request.groupIndex())
        .createdAt(Instant.now())
        .build();
  }
}
