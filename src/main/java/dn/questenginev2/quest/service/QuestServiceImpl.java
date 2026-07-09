package dn.questenginev2.quest.service;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.quest.dto.CreateQuestRequest;
import dn.questenginev2.quest.dto.QuestResponse;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestStatus;
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

    private final QuestRepository questRepository;
    private final UserService userService;

    @Override
    public QuestResponse createQuest(CreateQuestRequest request, Authentication auth) {
        User currentUser = userService.getCurrentUser(auth);
        validateAuthorOrAdmin(currentUser);

        Quest quest = Quest.builder()
                .title(request.getTitle())
                .description(request.getDescription() != null ? request.getDescription() : "")
                .type(request.getType())
                .status(QuestStatus.DRAFT)
                .createdAt(Instant.now())
                .build();

        Quest savedQuest = questRepository.save(quest);
        return mapToResponse(savedQuest);
    }

    @Override
    public QuestResponse getQuestById(Long questId) {
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new IllegalArgumentException("Квест не найден: " + questId));
        return mapToResponse(quest);
    }

    private void validateAuthorOrAdmin(User user) {
        if (user.getRole() != UserRole.AUTHOR && user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenOperationException("Создавать квесты могут только AUTHOR или ADMIN");
        }
    }

    private QuestResponse mapToResponse(Quest quest) {
        return QuestResponse.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .type(quest.getType())
                .status(quest.getStatus())
                .createdAt(quest.getCreatedAt())
                .startedAt(quest.getStartedAt())
                .endAt(quest.getEndAt())
                .build();
    }
}
