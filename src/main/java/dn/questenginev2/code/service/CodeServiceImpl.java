package dn.questenginev2.code.service;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.code.dto.CreateCodeRequest;
import dn.questenginev2.code.dto.CodeResponse;
import dn.questenginev2.code.entity.Code;
import dn.questenginev2.code.entity.CodeType;
import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.service.QuestService;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

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

        validateCodeValueUnique(request.getValue());

        Code code = buildCode(request, level);
        Code savedCode = codeRepository.save(code);

        return buildCodeResponse(savedCode);
    }

    @Override
    public List<CodeResponse> getCodesByLevelId(Long levelId) {
        Level level = validateLevelExist(levelId);
        return codeRepository.findByLevelIdOrderByCreatedAt(level.getId())
                .stream()
                .map(this::buildCodeResponse)
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

        if (!code.getValue().equals(request.getValue())) {
            validateCodeValueUnique(request.getValue());
        }

        code.setValue(request.getValue());
        code.setType(request.getType());
        code.setPoints(request.getPoints());

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
        return levelRepository.findById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Уровень не найден: " + levelId));
    }

    private Code validateCodeExist(Long codeId) {
        return codeRepository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("Код не найден: " + codeId));
    }

    private void validateCodeValueUnique(String value) {
        if (codeRepository.existsByValue(value)) {
            throw new IllegalArgumentException("Код уже существует: " + value);
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
                .createdAt(code.getCreatedAt())
                .build();
    }

    private Code buildCode(CreateCodeRequest request, Level level) {
        return Code.builder()
                .level(level)
                .value(request.getValue())
                .type(request.getType())
                .points(request.getPoints())
                .createdAt(Instant.now())
                .build();
    }
}
