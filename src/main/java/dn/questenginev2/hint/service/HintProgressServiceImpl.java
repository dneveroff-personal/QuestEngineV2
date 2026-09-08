package dn.questenginev2.hint.service;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.hint.dto.HintProgressResponse;
import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.hint.entity.HintProgress;
import dn.questenginev2.hint.entity.HintType;
import dn.questenginev2.hint.repository.HintProgressRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * ADR-0020 (REGULAR — auto-reveal, показ через Job 3), ADR-0021 (BONUS/PENALTY — явное взятие
 * командой через {@link #takeHint}).
 */
@Service
@Transactional
@AllArgsConstructor
public class HintProgressServiceImpl implements HintProgressService {

  private final HintProgressRepository hintProgressRepository;
  private final HintRepository hintRepository;
  private final QuestProgressRepository questProgressRepository;
  private final LevelProgressRepository levelProgressRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final UserService userService;
  private final Clock clock;

  @Autowired
  public HintProgressServiceImpl(
      HintProgressRepository hintProgressRepository,
      HintRepository hintRepository,
      QuestProgressRepository questProgressRepository,
      LevelProgressRepository levelProgressRepository,
      TeamMemberRepository teamMemberRepository,
      UserService userService) {
    this(
        hintProgressRepository,
        hintRepository,
        questProgressRepository,
        levelProgressRepository,
        teamMemberRepository,
        userService,
        Clock.systemUTC());
  }

  @Override
  public List<HintProgressResponse> getVisibleHints(
      Long questId, Long teamId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    QuestProgress questProgress = validateQuestProgressExist(questId, teamId);
    validateTeamMembership(currentUser, questProgress.getTeam());

    Optional<LevelProgress> activeLevelProgress =
        levelProgressRepository.findByQuestProgressIdAndStatus(
            questProgress.getId(), LevelProgressStatus.ACTIVE);

    if (activeLevelProgress.isEmpty()) {
      return List.of();
    }

    LevelProgress levelProgress = activeLevelProgress.get();
    Instant now = clock.instant();

    List<Hint> hints =
        hintRepository.findByLevelIdOrderByOrderIndex(levelProgress.getLevel().getId());
    Map<Long, HintProgress> takenByHintId =
        hintProgressRepository.findByLevelProgressIdOrderByShownAt(levelProgress.getId()).stream()
            .collect(Collectors.toMap(hp -> hp.getHint().getId(), hp -> hp));

    return hints.stream()
        .map(
            hint -> buildVisibleResponse(hint, levelProgress, takenByHintId.get(hint.getId()), now))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  @Override
  public HintProgressResponse takeHint(
      Long questId, Long teamId, Long hintId, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);
    QuestProgress questProgress = validateQuestProgressExist(questId, teamId);
    validateTeamMembership(currentUser, questProgress.getTeam());

    LevelProgress levelProgress =
        levelProgressRepository
            .findByQuestProgressIdAndStatus(questProgress.getId(), LevelProgressStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ForbiddenOperationException(
                        "У команды нет активного уровня для взятия подсказок"));

    Hint hint =
        hintRepository
            .findById(hintId)
            .orElseThrow(() -> new IllegalArgumentException("Подсказка не найдена: " + hintId));

    if (!hint.getLevel().getId().equals(levelProgress.getLevel().getId())) {
      throw new ForbiddenOperationException("Подсказка не относится к активному уровню команды");
    }

    Instant now = clock.instant();
    Instant hintAvailableAt = levelProgress.getOpenedAt().plusSeconds(hint.getDelaySeconds());
    if (now.isBefore(hintAvailableAt)) {
      throw new ForbiddenOperationException("Подсказка ещё не стала доступна");
    }

    Optional<HintProgress> existing =
        hintProgressRepository.findByLevelProgressIdAndHintId(levelProgress.getId(), hintId);
    if (existing.isPresent()) {
      return buildRevealedResponse(existing.get());
    }

    try {
      HintProgress saved =
          hintProgressRepository.saveAndFlush(
              HintProgress.builder().levelProgress(levelProgress).hint(hint).shownAt(now).build());
      return buildRevealedResponse(saved);
    } catch (DataIntegrityViolationException alreadyTakenConcurrently) {
      HintProgress raceWinner =
          hintProgressRepository
              .findByLevelProgressIdAndHintId(levelProgress.getId(), hintId)
              .orElseThrow(() -> alreadyTakenConcurrently);
      return buildRevealedResponse(raceWinner);
    }
  }

  private QuestProgress validateQuestProgressExist(Long questId, Long teamId) {
    return questProgressRepository
        .findByQuestIdAndTeamId(questId, teamId)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Прогресс команды не найден: questId=" + questId + ", teamId=" + teamId));
  }

  private void validateTeamMembership(User user, Team team) {
    if (teamMemberRepository.findByUserAndTeam(user, team).isEmpty()) {
      throw new ForbiddenOperationException("Видеть подсказки может только участник этой команды");
    }
  }

  /**
   * Три состояния (ADR-0020/ADR-0021): не наступило время -> Optional.empty (не включаем в
   * ответ); REGULAR показана или BONUS/PENALTY взята -> полный ответ; BONUS/PENALTY доступна, но
   * не взята -> ответ без content/bonusPenaltySeconds (только hintId/orderIndex/type).
   */
  private Optional<HintProgressResponse> buildVisibleResponse(
      Hint hint, LevelProgress levelProgress, HintProgress taken, Instant now) {
    Instant hintAvailableAt = levelProgress.getOpenedAt().plusSeconds(hint.getDelaySeconds());
    if (now.isBefore(hintAvailableAt)) {
      return Optional.empty();
    }

    if (taken != null) {
      return Optional.of(buildRevealedResponse(taken));
    }

    if (hint.getType() == HintType.REGULAR) {
      // REGULAR должна быть уже показана Job 3 к этому моменту (now >= hintAvailableAt) — если
      // почему-либо ещё не показана (небольшая гонка с планировщиком), не палим content раньше
      // времени — просто не включаем в ответ, следующий опрос клиента подхватит её после Job 3.
      return Optional.empty();
    }

    // BONUS/PENALTY, доступна, но не взята: видны только hintId/orderIndex/type (ADR-0021).
    return Optional.of(
        HintProgressResponse.builder()
            .hintId(hint.getId())
            .orderIndex(hint.getOrderIndex())
            .type(hint.getType())
            .content(null)
            .bonusPenaltySeconds(null)
            .shownAt(null)
            .build());
  }

  private HintProgressResponse buildRevealedResponse(HintProgress hintProgress) {
    Hint hint = hintProgress.getHint();
    return HintProgressResponse.builder()
        .hintId(hint.getId())
        .orderIndex(hint.getOrderIndex())
        .content(hint.getContent())
        .type(hint.getType())
        .bonusPenaltySeconds(hint.getBonusPenaltySeconds())
        .shownAt(hintProgress.getShownAt())
        .build();
  }
}
