package dn.questenginev2.code.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

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
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.service.QuestProgressService;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CodeSubmissionServiceImplTest {

  @Mock private CodeSubmissionRepository codeSubmissionRepository;
  @Mock private CodeRepository codeRepository;
  @Mock private QuestProgressRepository questProgressRepository;
  @Mock private LevelProgressRepository levelProgressRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private QuestProgressService questProgressService;
  @Mock private dn.questenginev2.user.service.UserService userService;
  @Mock private Clock clock;
  @Mock private Authentication authentication;

  @InjectMocks private CodeSubmissionServiceImpl codeSubmissionService;

  private User currentUser;
  private Team team;
  private Quest quest;
  private QuestProgress questProgress;
  private Level level;
  private LevelProgress levelProgress;
  private final Instant fixedNow = Instant.parse("2026-08-24T21:00:00Z");

  @BeforeEach
  void setUp() {
    currentUser = new User();
    currentUser.setId(1L);
    currentUser.setRole(UserRole.PLAYER);
    team = Team.builder().id(10L).name("Team A").captain(currentUser).build();
    quest =
        Quest.builder()
            .id(100L)
            .title("Test Quest")
            .description("Test")
            .type(QuestType.TEAM)
            .build();
    level = Level.builder().id(1000L).quest(quest).title("L1").orderIndex(1).build();
    questProgress =
        QuestProgress.builder()
            .id(500L)
            .quest(quest)
            .team(team)
            .status(QuestProgressStatus.RUNNING)
            .build();
    levelProgress =
        LevelProgress.builder()
            .id(2000L)
            .questProgress(questProgress)
            .level(level)
            .status(LevelProgressStatus.ACTIVE)
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(currentUser);
    when(questProgressRepository.findByQuestIdAndTeamId(100L, 10L))
        .thenReturn(Optional.of(questProgress));
    when(teamMemberRepository.findByUserAndTeam(currentUser, team))
        .thenReturn(Optional.of(TeamMember.builder().id(1L).user(currentUser).team(team).build()));
    when(levelProgressRepository.findFirstByQuestProgressIdOrderByOpenedAtDesc(500L))
        .thenReturn(Optional.of(levelProgress));
    when(clock.instant()).thenReturn(fixedNow);
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
  }

  private Code mainCode(Long id, String value, int codeIndex) {
    return Code.builder()
        .id(id)
        .level(level)
        .value(value)
        .type(CodeType.MAIN)
        .codeIndex(codeIndex)
        .build();
  }

  @Test
  void submitCode_returnsIncorrect_whenNoCodeMatches() {
    when(codeRepository.findByLevelIdOrderByCreatedAt(1000L))
        .thenReturn(List.of(mainCode(1L, "SINIY", 1)));

    CodeSubmissionResponse response =
        codeSubmissionService.submitCode(100L, 10L, new SubmitCodeRequest("wrong"), authentication);

    assertThat(response.getResult()).isEqualTo(CodeSubmissionResult.INCORRECT);
    assertThat(response.isLevelCompleted()).isFalse();
    verify(levelProgressRepository, never())
        .tryCompleteByCodesThreshold(anyLong(), anyLong(), any());
  }

  @Test
  void submitCode_isCaseInsensitiveAndTrimmed() {
    level.setRequiredMainCodesCount(1);
    when(codeRepository.findByLevelIdOrderByCreatedAt(1000L))
        .thenReturn(List.of(mainCode(1L, "SiNiY", 1)));
    when(codeSubmissionRepository.countDistinctSolvedCodeIndexes(2000L)).thenReturn(1L);
    when(levelProgressRepository.tryCompleteByCodesThreshold(2000L, 1L, fixedNow)).thenReturn(1);
    when(questProgressService.advanceAfterLevelCompleted(any()))
        .thenReturn(QuestProgressResponse.builder().status(QuestProgressStatus.RUNNING).build());
    when(levelProgressRepository.findById(2000L)).thenReturn(Optional.of(levelProgress));

    CodeSubmissionResponse response =
        codeSubmissionService.submitCode(
            100L, 10L, new SubmitCodeRequest("  siniy  "), authentication);

    assertThat(response.getResult()).isEqualTo(CodeSubmissionResult.CORRECT_MAIN);
    assertThat(response.isLevelCompleted()).isTrue();
  }

  @Test
  void submitCode_doesNotCompleteLevel_whenThresholdNotYetReached() {
    level.setRequiredMainCodesCount(2);
    when(codeRepository.findByLevelIdOrderByCreatedAt(1000L))
        .thenReturn(List.of(mainCode(1L, "siniy", 1), mainCode(2L, "dekabr", 2)));
    when(codeSubmissionRepository.countDistinctSolvedCodeIndexes(2000L)).thenReturn(1L);
    when(levelProgressRepository.tryCompleteByCodesThreshold(2000L, 2L, fixedNow)).thenReturn(0);

    CodeSubmissionResponse response =
        codeSubmissionService.submitCode(100L, 10L, new SubmitCodeRequest("siniy"), authentication);

    assertThat(response.getResult()).isEqualTo(CodeSubmissionResult.CORRECT_MAIN);
    assertThat(response.isLevelCompleted()).isFalse();
    assertThat(response.getRemainingMainCodes()).isEqualTo(1);
    verify(questProgressService, never()).advanceAfterLevelCompleted(any());
  }

  @Test
  void submitCode_completesLevelAndFinishesQuest_whenLastLevel() {
    level.setRequiredMainCodesCount(1);
    when(codeRepository.findByLevelIdOrderByCreatedAt(1000L))
        .thenReturn(List.of(mainCode(1L, "siniy", 1)));
    when(codeSubmissionRepository.countDistinctSolvedCodeIndexes(2000L)).thenReturn(1L);
    when(levelProgressRepository.tryCompleteByCodesThreshold(2000L, 1L, fixedNow)).thenReturn(1);
    when(levelProgressRepository.findById(2000L)).thenReturn(Optional.of(levelProgress));
    when(questProgressService.advanceAfterLevelCompleted(levelProgress))
        .thenReturn(QuestProgressResponse.builder().status(QuestProgressStatus.FINISHED).build());

    CodeSubmissionResponse response =
        codeSubmissionService.submitCode(100L, 10L, new SubmitCodeRequest("siniy"), authentication);

    assertThat(response.isLevelCompleted()).isTrue();
    assertThat(response.isQuestFinished()).isTrue();
    assertThat(response.getRemainingMainCodes()).isZero();
  }

  @Test
  void submitCode_returnsCorrectBonus_andDoesNotAttemptCompletion_forBonusCode() {
    Code bonusCode = Code.builder().id(9L).level(level).value("bonus").type(CodeType.BONUS).build();
    when(codeRepository.findByLevelIdOrderByCreatedAt(1000L)).thenReturn(List.of(bonusCode));

    CodeSubmissionResponse response =
        codeSubmissionService.submitCode(100L, 10L, new SubmitCodeRequest("bonus"), authentication);

    assertThat(response.getResult()).isEqualTo(CodeSubmissionResult.CORRECT_BONUS);
    assertThat(response.isLevelCompleted()).isFalse();
    verify(levelProgressRepository, never())
        .tryCompleteByCodesThreshold(anyLong(), anyLong(), any());
  }

  @Test
  void submitCode_savesSubmissionRecord_regardlessOfResult() {
    when(codeRepository.findByLevelIdOrderByCreatedAt(1000L)).thenReturn(List.of());

    codeSubmissionService.submitCode(100L, 10L, new SubmitCodeRequest("anything"), authentication);

    ArgumentCaptor<CodeSubmission> captor = ArgumentCaptor.forClass(CodeSubmission.class);
    verify(codeSubmissionRepository).save(captor.capture());
    assertThat(captor.getValue().getResult()).isEqualTo(CodeSubmissionResult.INCORRECT);
    assertThat(captor.getValue().getRawValue()).isEqualTo("anything");
    assertThat(captor.getValue().getSubmittedBy()).isEqualTo(currentUser);
  }

  @Test
  void submitCode_throwsForbiddenOperationException_whenUserNotTeamMember() {
    when(teamMemberRepository.findByUserAndTeam(currentUser, team)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                codeSubmissionService.submitCode(
                    100L, 10L, new SubmitCodeRequest("siniy"), authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("участник");
  }

  @Test
  void submitCode_throwsForbiddenOperationException_whenNoActiveLevelProgress() {
    when(levelProgressRepository.findFirstByQuestProgressIdOrderByOpenedAtDesc(500L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                codeSubmissionService.submitCode(
                    100L, 10L, new SubmitCodeRequest("siniy"), authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("активного уровня");
  }

  @Test
  void submitCode_throwsIllegalArgumentException_whenQuestProgressNotFound() {
    when(questProgressRepository.findByQuestIdAndTeamId(100L, 10L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                codeSubmissionService.submitCode(
                    100L, 10L, new SubmitCodeRequest("siniy"), authentication))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void submitCode_remainingMainCodesIsNull_whenLevelHasNoMainCodes() {
    when(codeRepository.findByLevelIdOrderByCreatedAt(1000L)).thenReturn(List.of());

    CodeSubmissionResponse response =
        codeSubmissionService.submitCode(100L, 10L, new SubmitCodeRequest("x"), authentication);

    assertThat(response.getRemainingMainCodes()).isNull();
  }

  @Test
  void submitCode_usesDefaultRequiredCount_whenLevelRequiredMainCodesCountNotSet() {
    // 2 коды на уровне, порог не задан -> требуются оба (ADR-0005: null = все коды).
    level.setRequiredMainCodesCount(null);
    when(codeRepository.findByLevelIdOrderByCreatedAt(1000L))
        .thenReturn(List.of(mainCode(1L, "siniy", 1), mainCode(2L, "dekabr", 2)));
    when(codeSubmissionRepository.countDistinctSolvedCodeIndexes(2000L)).thenReturn(1L);

    CodeSubmissionResponse response =
        codeSubmissionService.submitCode(100L, 10L, new SubmitCodeRequest("siniy"), authentication);

    verify(levelProgressRepository).tryCompleteByCodesThreshold(2000L, 2L, fixedNow);
    assertThat(response.getRemainingMainCodes()).isEqualTo(1);
  }
}
