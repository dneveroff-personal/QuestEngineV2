package dn.questenginev2.level.service;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.level.dto.CreateLevelRequest;
import dn.questenginev2.level.dto.LevelResponse;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.service.QuestService;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Transactional
@AllArgsConstructor
public class LevelServiceImpl implements LevelService {

    private final LevelRepository levelRepository;
    private final QuestService questService;
    private final UserService userService;

    // ────── IMPLEMENTATIONS ───────────────────────────────────────────────────────────
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
    public Integer getMaxLevelIndex(Long questId) {
        Integer maxIndex = levelRepository.findMaxOrderIndex(questId);
        return maxIndex != null ? maxIndex : 0;
    }

    // ────── VALIDATIONS ───────────────────────────────────────────────────────────


    // ────── BUILDERS ───────────────────────────────────────────────────────────
    private LevelResponse buildLevelResponse(Level level) {
        return LevelResponse.builder()
                .id(level.getId())
                .questId(level.getQuest().getId())
                .title(level.getTitle())
                .orderIndex(level.getOrderIndex())
                .content(level.getContent())
                .createdAt(level.getCreatedAt())
                .updatedAt(level.getUpdatedAt())
                .build();
    }

    private Level buildLevel(CreateLevelRequest request, Quest quest) {
        return Level.builder()
                .quest(quest)
                .title(request.getTitle())
                .orderIndex(getMaxLevelIndex(quest.getId()) + 1)
                .content(request.getContent())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
