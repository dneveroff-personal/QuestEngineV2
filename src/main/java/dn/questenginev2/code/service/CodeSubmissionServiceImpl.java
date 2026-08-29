package dn.questenginev2.code.service;

import dn.questenginev2.code.dto.CodeSubmissionResponse;
import dn.questenginev2.code.dto.SubmitCodeRequest;
import dn.questenginev2.code.entity.Code;
import dn.questenginev2.code.entity.CodeSubmission;
import dn.questenginev2.code.entity.CodeSubmissionResult;
import dn.questenginev2.code.entity.CodeType;
import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.code.repository.CodeSubmissionRepository;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.quest.dto.QuestProgressResponse;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.service.QuestProgressService;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Ввод кода командой во время игры. См. docs/01-domain/code-submission.md, ADR-0004/0005/0006.
 *
 * <p>Rate limiting здесь намеренно НЕ применяется (ADR-0016, 05-security/threat-model.md) —
 * скорость ввода кодов является частью игровой механики, а не признаком атаки.
 */
@Service
@Transactional
@AllArgsConstructor
public class CodeSubmissionServiceImpl implements CodeSubmissionService {

  private final CodeSubmissionRepository codeSubmissionRepository;
  private final CodeRepository codeRepository;
  private final QuestProgressRepository questProgressRepository;
  private final LevelProgressRepository levelProgressRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final QuestProgressService questProgressService;
  private final UserService userService;
  private final Clock clock;

  @Autowired
  public CodeSubmissionServiceImpl(
      CodeSubmissionRepository codeSubmissionRepository,
      CodeRepository codeRepository,
      QuestProgressRepository questProgressRepository,
      LevelProgressRepository levelProgressRepository,
      TeamMemberRepository teamMemberRepository,
      QuestProgressService questProgressService,
      UserService userService) {
    this(
        codeSubmissionRepository,
        codeRepository,
        questProgressRepository,
        levelProgressRepository,
        teamMemberRepository,
        questProgressService,
        userService,
        Clock.systemUTC());
  }

  @Override
  public CodeSubmissionResponse submitCode(
      Long questId, Long teamId, SubmitCodeRequest request, Authentication auth) {
    User currentUser = userService.getCurrentUser(auth);

    QuestProgress questProgress = validateQuestProgressExist(questId, teamId);
    validateTeamMembership(currentUser, questProgress.getTeam());

    LevelProgress levelProgress =
        levelProgressRepository
            .findByQuestProgressIdAndStatus(questProgress.getId(), LevelProgressStatus.ACTIVE)
            .orElseThrow(
                () ->
                    new ForbiddenOperationException(
                        "У команды нет активного уровня для ввода кодов"));

    Level level = levelProgress.getLevel();
    List<Code> levelCodes = codeRepository.findByLevelIdOrderByCreatedAt(level.getId());

    Code matchedCode = matchCode(levelCodes, request.value());
    CodeSubmissionResult result = resolveResult(matchedCode);
    Instant now = clock.instant();

    CodeSubmission submission =
        CodeSubmission.builder()
            .levelProgress(levelProgress)
            .submittedBy(currentUser)
            .rawValue(request.value())
            .matchedCode(matchedCode)
            .result(result)
            .submittedAt(now)
            .build();
    codeSubmissionRepository.save(submission);

    Integer remainingMainCodes =
        computeRemainingMainCodes(level, levelCodes, levelProgress.getId());
    boolean levelCompleted = false;
    boolean questFinished = false;

    if (result == CodeSubmissionResult.CORRECT_MAIN) {
      long requiredCount = resolveRequiredCount(level, levelCodes);
      int updatedRows =
          levelProgressRepository.tryCompleteByCodesThreshold(
              levelProgress.getId(), requiredCount, now);

      if (updatedRows == 1) {
        levelCompleted = true;
        remainingMainCodes = 0;

        LevelProgress completedLevelProgress =
            levelProgressRepository
                .findById(levelProgress.getId())
                .orElseThrow(
                    () -> new IllegalStateException("LevelProgress исчез во время обработки"));
        QuestProgressResponse advanceResponse =
            questProgressService.advanceAfterLevelCompleted(completedLevelProgress);
        questFinished = advanceResponse.getStatus() == QuestProgressStatus.FINISHED;
      }
    }

    return CodeSubmissionResponse.builder()
        .result(result)
        .remainingMainCodes(remainingMainCodes)
        .levelCompleted(levelCompleted)
        .questFinished(questFinished)
        .submittedAt(now)
        .build();
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
      throw new ForbiddenOperationException("Вводить коды может только участник этой команды");
    }
  }

  /** Нормализация: регистронезависимо, обрезка пробелов по краям (01-domain/code-submission.md). */
  private String normalize(String value) {
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private Code matchCode(List<Code> levelCodes, String rawValue) {
    String normalizedInput = normalize(rawValue);
    return levelCodes.stream()
        .filter(code -> normalize(code.getValue()).equals(normalizedInput))
        .findFirst()
        .orElse(null);
  }

  private CodeSubmissionResult resolveResult(Code matchedCode) {
    if (matchedCode == null) {
      return CodeSubmissionResult.INCORRECT;
    }
    return switch (matchedCode.getType()) {
      case MAIN -> CodeSubmissionResult.CORRECT_MAIN;
      case BONUS -> CodeSubmissionResult.CORRECT_BONUS;
      case PENALTY -> CodeSubmissionResult.CORRECT_PENALTY;
    };
  }

  private long resolveRequiredCount(Level level, List<Code> levelCodes) {
    if (level.getRequiredMainCodesCount() != null) {
      return level.getRequiredMainCodesCount();
    }
    return countDistinctMainCodeIndexes(levelCodes);
  }

  /**
   * "Осталось ввести N из M кодов" (01-domain/code-submission.md, ADR-0005). Null, если на
   * уровне нет обязательных кодов вообще (например, уровень-заглушка по времени).
   */
  private Integer computeRemainingMainCodes(
      Level level, List<Code> levelCodes, Long levelProgressId) {
    long totalMainCodes = countDistinctMainCodeIndexes(levelCodes);
    if (totalMainCodes == 0) {
      return null;
    }
    long required =
        level.getRequiredMainCodesCount() != null
            ? level.getRequiredMainCodesCount()
            : totalMainCodes;
    long solved = codeSubmissionRepository.countDistinctSolvedCodeIndexes(levelProgressId);
    return (int) Math.max(0, required - solved);
  }

  private long countDistinctMainCodeIndexes(List<Code> levelCodes) {
    return levelCodes.stream()
        .filter(code -> code.getType() == CodeType.MAIN)
        .map(Code::getCodeIndex)
        .distinct()
        .count();
  }
}
