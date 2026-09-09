package dn.questenginev2.bonuspenalty.service;

import dn.questenginev2.bonuspenalty.dto.CreateManualTimeAdjustmentRequest;
import dn.questenginev2.bonuspenalty.dto.ManualTimeAdjustmentResponse;
import dn.questenginev2.bonuspenalty.entity.ManualTimeAdjustment;
import dn.questenginev2.bonuspenalty.repository.ManualTimeAdjustmentRepository;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.repository.QuestProgressRepository;
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
public class ManualTimeAdjustmentServiceImpl implements ManualTimeAdjustmentService {

  private final ManualTimeAdjustmentRepository manualTimeAdjustmentRepository;
  private final QuestProgressRepository questProgressRepository;
  private final QuestService questService;
  private final UserService userService;

  @Override
  public ManualTimeAdjustmentResponse create(
      Long questProgressId, CreateManualTimeAdjustmentRequest request, Authentication auth) {

    User currentUser = userService.getCurrentUser(auth);

    QuestProgress questProgress = validateQuestProgressExist(questProgressId);

    questService.validateAuthorOrAdmin(currentUser);
    questService.validateQuestAuthor(currentUser, questProgress.getQuest().getId());

    ManualTimeAdjustment adjustment =
        ManualTimeAdjustment.builder()
            .questProgress(questProgress)
            .type(request.type())
            .seconds(request.seconds())
            .reason(request.reason())
            .createdBy(currentUser)
            .createdAt(Instant.now())
            .build();

    ManualTimeAdjustment saved = manualTimeAdjustmentRepository.save(adjustment);

    return buildResponse(saved);
  }

  @Override
  public ManualTimeAdjustmentResponse revoke(Long adjustmentId, Authentication auth) {

    User currentUser = userService.getCurrentUser(auth);

    ManualTimeAdjustment adjustment = validateAdjustmentExist(adjustmentId);

    questService.validateAuthorOrAdmin(currentUser);
    questService.validateQuestAuthor(currentUser, adjustment.getQuestProgress().getQuest().getId());

    if (adjustment.getRevokedAt() != null) {
      throw new IllegalArgumentException("Корректировка уже отозвана: " + adjustmentId);
    }

    // bonus-penalty.md, "Ручная корректировка": отзыв запрещён после
    // официального завершения Quest — иначе итоговая статистика/место
    // менялись бы задним числом уже после того, как результат объявлен.
    if (adjustment.getQuestProgress().getQuest().getStatus() == QuestStatus.FINISHED) {
      throw new ForbiddenOperationException(
          "Нельзя отозвать корректировку после завершения квеста: " + adjustmentId);
    }

    adjustment.setRevokedAt(Instant.now());
    adjustment.setRevokedBy(currentUser);

    ManualTimeAdjustment saved = manualTimeAdjustmentRepository.save(adjustment);

    return buildResponse(saved);
  }

  @Override
  public List<ManualTimeAdjustmentResponse> getByQuestProgressId(Long questProgressId) {

    validateQuestProgressExist(questProgressId);

    return manualTimeAdjustmentRepository
        .findByQuestProgressIdOrderByCreatedAt(questProgressId)
        .stream()
        .map(this::buildResponse)
        .toList();
  }

  private QuestProgress validateQuestProgressExist(Long questProgressId) {
    return questProgressRepository
        .findById(questProgressId)
        .orElseThrow(
            () -> new IllegalArgumentException("Прогресс квеста не найден: " + questProgressId));
  }

  private ManualTimeAdjustment validateAdjustmentExist(Long adjustmentId) {
    return manualTimeAdjustmentRepository
        .findById(adjustmentId)
        .orElseThrow(
            () -> new IllegalArgumentException("Корректировка не найдена: " + adjustmentId));
  }

  private ManualTimeAdjustmentResponse buildResponse(ManualTimeAdjustment adjustment) {

    return ManualTimeAdjustmentResponse.builder()
        .id(adjustment.getId())
        .questProgressId(adjustment.getQuestProgress().getId())
        .type(adjustment.getType())
        .seconds(adjustment.getSeconds())
        .reason(adjustment.getReason())
        .createdByUserId(adjustment.getCreatedBy().getId())
        .createdAt(adjustment.getCreatedAt())
        .revokedAt(adjustment.getRevokedAt())
        .revokedByUserId(
            adjustment.getRevokedBy() != null ? adjustment.getRevokedBy().getId() : null)
        .build();
  }
}
