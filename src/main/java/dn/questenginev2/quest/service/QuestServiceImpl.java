package dn.questenginev2.quest.service;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.quest.dto.CreateQuestRequest;
import dn.questenginev2.quest.dto.QuestResponse;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestAuthor;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestRepository;
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
public class QuestServiceImpl implements QuestService {

    private final QuestAuthorRepository questAuthorRepository;
    private final QuestRepository questRepository;
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
    public QuestResponse updateQuest(Long questId, CreateQuestRequest request, Authentication auth) {
        User currentUser = userService.getCurrentUser(auth);
        validateAuthorOrAdmin(currentUser);
        Quest quest = validateQuestExist(questId);
        validateQuestAuthor(currentUser, questId);

        quest.setTitle(request.getTitle());
        quest.setDescription(request.getDescription());
        quest.setType(request.getType());
        quest.setStartTime(request.getStartTime());
        quest.setFinishTime(request.getFinishTime());

        Quest savedQuest = questRepository.save(quest);
        return buildQuestResponse(savedQuest);
    }

    @Override
    public void delete(Long questId) {
        Quest quest = validateQuestExist(questId);
        questRepository.delete(quest);
    }

    // ────── VALIDATIONS ───────────────────────────────────────────────────────────
    @Override
    public Quest validateQuestExist(Long questId) {
        return questRepository.findById(questId)
                .orElseThrow(() -> new IllegalArgumentException("Квест не найден: " + questId));
    }

    @Override
    public void validateAuthorOrAdmin(User user) {
        if (user.getRole() != UserRole.AUTHOR && user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenOperationException("Доступ к редактированию квестов имеют только AUTHOR или ADMIN");
        }
    }

    @Override
    public void validateQuestAuthor(User user, Long questId) {
        if (user.getRole() != UserRole.ADMIN && !questAuthorRepository.existsByQuestIdAndUserId(questId, user.getId())) {
            throw new ForbiddenOperationException("Редактировать квесты могут только Авторы");
        }
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
                .title(request.getTitle())
                .description(request.getDescription() != null ? request.getDescription() : "")
                .type(request.getType())
                .status(QuestStatus.DRAFT)
                .createdAt(Instant.now())
                .startTime(request.getStartTime())
                .finishTime(request.getFinishTime())
                .build();
    }

    private QuestAuthor buildQuestAuthor(Quest savedQuest, User currentUser) {
        return QuestAuthor.builder()
                .quest(savedQuest)
                .user(currentUser)
                .build();
    }
}
