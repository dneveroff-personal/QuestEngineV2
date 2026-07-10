package dn.questenginev2.hint.service;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.hint.dto.CreateHintRequest;
import dn.questenginev2.hint.dto.HintResponse;
import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.hint.repository.HintRepository;
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

        Hint hint = buildHint(request, level);
        Hint savedHint = hintRepository.save(hint);

        return buildHintResponse(savedHint);
    }

    @Override
    public List<HintResponse> getHintsByLevelId(Long levelId) {
        Level level = validateLevelExist(levelId);
        return hintRepository.findByLevelIdOrderByOrderIndex(level.getId())
                .stream()
                .map(this::buildHintResponse)
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

        hint.setOrderIndex(request.getOrderIndex());
        hint.setDelaySeconds(request.getDelaySeconds());
        hint.setContent(request.getContent());
        hint.setUpdatedAt(Instant.now());

        Hint savedHint = hintRepository.save(hint);
        return buildHintResponse(savedHint);
    }

    @Override
    public void deleteHint(Long hintId) {
        Hint hint = validateHintExist(hintId);
        hintRepository.delete(hint);
    }

    @Override
    public Integer getMaxHintIndex(Long levelId) {
        Integer maxIndex = hintRepository.findMaxOrderIndex(levelId);
        return maxIndex != null ? maxIndex : 0;
    }

    // ────── VALIDATIONS ───────────────────────────────────────────────────────────
    private Level validateLevelExist(Long levelId) {
        return levelRepository.findById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Уровень не найден: " + levelId));
    }

    private Hint validateHintExist(Long hintId) {
        return hintRepository.findById(hintId)
                .orElseThrow(() -> new IllegalArgumentException("Подсказка не найдена: " + hintId));
    }

    // ────── BUILDERS ───────────────────────────────────────────────────────────
    private HintResponse buildHintResponse(Hint hint) {
        return HintResponse.builder()
                .id(hint.getId())
                .levelId(hint.getLevel().getId())
                .orderIndex(hint.getOrderIndex())
                .delaySeconds(hint.getDelaySeconds())
                .content(hint.getContent())
                .createdAt(hint.getCreatedAt())
                .updatedAt(hint.getUpdatedAt())
                .build();
    }

    private Hint buildHint(CreateHintRequest request, Level level) {
        return Hint.builder()
                .level(level)
                .orderIndex(request.getOrderIndex())
                .delaySeconds(request.getDelaySeconds())
                .content(request.getContent())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
