package dn.questenginev2.hint.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.hint.dto.HintProgressResponse;
import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.hint.entity.HintProgress;
import dn.questenginev2.hint.entity.HintType;
import dn.questenginev2.hint.repository.HintProgressRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HintProgressServiceImplTest {

  @Mock private HintProgressRepository hintProgressRepository;
  @Mock private HintRepository hintRepository;
  @Mock private QuestProgressRepository questProgressRepository;
  @Mock private LevelProgressRepository levelProgressRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private UserService userService;
  @Mock private Clock clock;
  @Mock private Authentication authentication;

  @InjectMocks private HintProgressServiceImpl hintProgressService;

  private User currentUser;
  private Team team;
  private QuestProgress questProgress;
  private Level level;
  private LevelProgress levelProgress;
  private final Instant fixedNow = Instant.parse("2026-08-24T21:10:00Z");

  @BeforeEach
  void setUp() {
    currentUser = new User();
    currentUser.setId(1L);
    currentUser.setRole(UserRole.PLAYER);

    team = Team.builder().id(10L).name("Team A").captain(currentUser).build();

    Quest quest =
        Quest.builder().id(100L).title("Quest").description("D").type(QuestType.TEAM).build();
    questProgress = QuestProgress.builder().id(500L).quest(quest).team(team).build();
    level = Level.builder().id(1000L).quest(quest).title("L1").orderIndex(1).build();
    levelProgress =
        LevelProgress.builder()
            .id(2000L)
            .questProgress(questProgress)
            .level(level)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(fixedNow.minusSeconds(120))
            .build();

    when(clock.instant()).thenReturn(fixedNow);
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    when(userService.getCurrentUser(authentication)).thenReturn(currentUser);
    when(questProgressRepository.findByQuestIdAndTeamId(100L, 10L))
        .thenReturn(Optional.of(questProgress));
    when(teamMemberRepository.findByUserAndTeam(currentUser, team))
        .thenReturn(Optional.of(TeamMember.builder().id(1L).user(currentUser).team(team).build()));
  }

  private Hint hint(Long id, int orderIndex, int delaySeconds, HintType type, Integer seconds) {
    return Hint.builder()
        .id(id)
        .level(level)
        .orderIndex(orderIndex)
        .delaySeconds(delaySeconds)
        .content("Content " + id)
        .type(type)
        .bonusPenaltySeconds(seconds)
        .build();
  }

  // ────── getVisibleHints ────────────────────────────────────────────────────

  @Test
  void getVisibleHints_returnsEmptyList_whenNoActiveLevel() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.empty());

    List<HintProgressResponse> response =
        hintProgressService.getVisibleHints(100L, 10L, authentication);

    assertThat(response).isEmpty();
  }

  @Test
  void getVisibleHints_omitsHint_whenDelayNotYetElapsed() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    Hint futureHint = hint(1L, 1, 3600, HintType.REGULAR, null); // openedAt + 3600s = в будущем
    when(hintRepository.findByLevelIdOrderByOrderIndex(1000L)).thenReturn(List.of(futureHint));
    when(hintProgressRepository.findByLevelProgressIdOrderByShownAt(2000L)).thenReturn(List.of());

    List<HintProgressResponse> response =
        hintProgressService.getVisibleHints(100L, 10L, authentication);

    assertThat(response).isEmpty();
  }

  @Test
  void getVisibleHints_returnsFullContent_forRegularHintAlreadyShown() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    Hint regularHint = hint(1L, 1, 60, HintType.REGULAR, null);
    HintProgress shown =
        HintProgress.builder()
            .id(1L)
            .levelProgress(levelProgress)
            .hint(regularHint)
            .shownAt(fixedNow)
            .build();
    when(hintRepository.findByLevelIdOrderByOrderIndex(1000L)).thenReturn(List.of(regularHint));
    when(hintProgressRepository.findByLevelProgressIdOrderByShownAt(2000L))
        .thenReturn(List.of(shown));

    List<HintProgressResponse> response =
        hintProgressService.getVisibleHints(100L, 10L, authentication);

    assertThat(response).hasSize(1);
    assertThat(response.get(0).getContent()).isEqualTo("Content 1");
    assertThat(response.get(0).getType()).isEqualTo(HintType.REGULAR);
  }

  @Test
  void getVisibleHints_omitsRegularHint_whenDueButNotYetShownByScheduler() {
    // Небольшая гонка с Job 3 — не палим content раньше времени, просто не включаем в ответ.
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    Hint regularHint = hint(1L, 1, 60, HintType.REGULAR, null);
    when(hintRepository.findByLevelIdOrderByOrderIndex(1000L)).thenReturn(List.of(regularHint));
    when(hintProgressRepository.findByLevelProgressIdOrderByShownAt(2000L)).thenReturn(List.of());

    List<HintProgressResponse> response =
        hintProgressService.getVisibleHints(100L, 10L, authentication);

    assertThat(response).isEmpty();
  }

  @Test
  void getVisibleHints_returnsTypeOnly_forBonusHintAvailableButNotTaken() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    Hint bonusHint = hint(1L, 1, 60, HintType.BONUS, 60);
    when(hintRepository.findByLevelIdOrderByOrderIndex(1000L)).thenReturn(List.of(bonusHint));
    when(hintProgressRepository.findByLevelProgressIdOrderByShownAt(2000L)).thenReturn(List.of());

    List<HintProgressResponse> response =
        hintProgressService.getVisibleHints(100L, 10L, authentication);

    assertThat(response).hasSize(1);
    HintProgressResponse visible = response.get(0);
    assertThat(visible.getType()).isEqualTo(HintType.BONUS);
    assertThat(visible.getContent()).isNull();
    assertThat(visible.getBonusPenaltySeconds()).isNull();
    assertThat(visible.getShownAt()).isNull();
  }

  @Test
  void getVisibleHints_returnsFullContent_forPenaltyHintAlreadyTaken() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    Hint penaltyHint = hint(1L, 1, 60, HintType.PENALTY, 600);
    HintProgress taken =
        HintProgress.builder()
            .id(1L)
            .levelProgress(levelProgress)
            .hint(penaltyHint)
            .shownAt(fixedNow)
            .build();
    when(hintRepository.findByLevelIdOrderByOrderIndex(1000L)).thenReturn(List.of(penaltyHint));
    when(hintProgressRepository.findByLevelProgressIdOrderByShownAt(2000L))
        .thenReturn(List.of(taken));

    List<HintProgressResponse> response =
        hintProgressService.getVisibleHints(100L, 10L, authentication);

    assertThat(response).hasSize(1);
    assertThat(response.get(0).getContent()).isEqualTo("Content 1");
    assertThat(response.get(0).getBonusPenaltySeconds()).isEqualTo(600);
  }

  @Test
  void getVisibleHints_throwsForbiddenOperationException_whenUserNotTeamMember() {
    when(teamMemberRepository.findByUserAndTeam(currentUser, team)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hintProgressService.getVisibleHints(100L, 10L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("участник");
  }

  @Test
  void getVisibleHints_throwsIllegalArgumentException_whenQuestProgressNotFound() {
    when(questProgressRepository.findByQuestIdAndTeamId(100L, 10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hintProgressService.getVisibleHints(100L, 10L, authentication))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ────── takeHint ───────────────────────────────────────────────────────────

  @Test
  void takeHint_revealsContentAndCreatesHintProgress_whenAvailable() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    Hint penaltyHint = hint(1L, 1, 60, HintType.PENALTY, 600);
    when(hintRepository.findById(1L)).thenReturn(Optional.of(penaltyHint));
    when(hintProgressRepository.findByLevelProgressIdAndHintId(2000L, 1L))
        .thenReturn(Optional.empty());
    when(hintProgressRepository.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    HintProgressResponse response = hintProgressService.takeHint(100L, 10L, 1L, authentication);

    assertThat(response.getContent()).isEqualTo("Content 1");
    assertThat(response.getBonusPenaltySeconds()).isEqualTo(600);
    assertThat(response.getShownAt()).isEqualTo(fixedNow);
  }

  @Test
  void takeHint_isIdempotent_whenAlreadyTaken() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    Hint penaltyHint = hint(1L, 1, 60, HintType.PENALTY, 600);
    HintProgress existing =
        HintProgress.builder()
            .id(9L)
            .levelProgress(levelProgress)
            .hint(penaltyHint)
            .shownAt(fixedNow.minusSeconds(30))
            .build();
    when(hintRepository.findById(1L)).thenReturn(Optional.of(penaltyHint));
    when(hintProgressRepository.findByLevelProgressIdAndHintId(2000L, 1L))
        .thenReturn(Optional.of(existing));

    HintProgressResponse response = hintProgressService.takeHint(100L, 10L, 1L, authentication);

    assertThat(response.getShownAt()).isEqualTo(fixedNow.minusSeconds(30));
    verify(hintProgressRepository, never()).saveAndFlush(any());
  }

  @Test
  void takeHint_throwsForbiddenOperationException_whenNotYetAvailable() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    Hint futureHint = hint(1L, 1, 3600, HintType.PENALTY, 600); // openedAt + 3600s = будущее
    when(hintRepository.findById(1L)).thenReturn(Optional.of(futureHint));

    assertThatThrownBy(() -> hintProgressService.takeHint(100L, 10L, 1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("не стала доступна");

    verify(hintProgressRepository, never()).saveAndFlush(any());
  }

  @Test
  void takeHint_throwsForbiddenOperationException_whenNoActiveLevel() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> hintProgressService.takeHint(100L, 10L, 1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("активного уровня");
  }

  @Test
  void takeHint_throwsForbiddenOperationException_whenHintBelongsToDifferentLevel() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    Level otherLevel = Level.builder().id(9999L).title("Other").orderIndex(2).build();
    Hint otherLevelHint =
        Hint.builder()
            .id(1L)
            .level(otherLevel)
            .orderIndex(1)
            .delaySeconds(60)
            .content("X")
            .type(HintType.PENALTY)
            .bonusPenaltySeconds(600)
            .build();
    when(hintRepository.findById(1L)).thenReturn(Optional.of(otherLevelHint));

    assertThatThrownBy(() -> hintProgressService.takeHint(100L, 10L, 1L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("не относится");
  }

  @Test
  void takeHint_throwsIllegalArgumentException_whenHintDoesNotExist() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    when(hintRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hintProgressService.takeHint(100L, 10L, 999L, authentication))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
