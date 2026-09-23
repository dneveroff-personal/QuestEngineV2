package dn.questenginev2.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dn.questenginev2.code.repository.CodeSubmissionRepository;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.ResourceNotFoundException;
import dn.questenginev2.hint.dto.HintProgressResponse;
import dn.questenginev2.hint.service.HintProgressService;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.quest.dto.CurrentLevelResponse;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class CurrentLevelServiceImplTest {

  @Mock private QuestProgressRepository questProgressRepository;
  @Mock private LevelProgressRepository levelProgressRepository;
  @Mock private CodeSubmissionRepository codeSubmissionRepository;
  @Mock private HintProgressService hintProgressService;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private UserService userService;
  @Mock private Authentication authentication;

  @InjectMocks private CurrentLevelServiceImpl currentLevelService;

  private User player;
  private User admin;
  private User outsider;
  private Team team;
  private Quest quest;
  private QuestProgress questProgress;
  private Level level;
  private LevelProgress levelProgress;

  private final Instant opened = Instant.parse("2026-01-01T12:00:00Z");
  private final Instant autoAt = Instant.parse("2026-01-01T12:30:00Z");

  @BeforeEach
  void setUp() {
    player = new User();
    player.setId(1L);
    player.setUsername("player");
    player.setRole(UserRole.PLAYER);

    admin = new User();
    admin.setId(2L);
    admin.setUsername("admin");
    admin.setRole(UserRole.ADMIN);

    outsider = new User();
    outsider.setId(3L);
    outsider.setUsername("outsider");
    outsider.setRole(UserRole.PLAYER);

    team = Team.builder().id(10L).name("Team X").build();
    quest =
        Quest.builder()
            .id(100L)
            .title("Quest")
            .type(QuestType.TEAM)
            .status(QuestStatus.RUNNING)
            .createdAt(Instant.now())
            .build();
    questProgress =
        QuestProgress.builder()
            .id(50L)
            .quest(quest)
            .team(team)
            .status(QuestProgressStatus.RUNNING)
            .questStartedAt(Instant.now())
            .build();
    level =
        Level.builder()
            .id(20L)
            .quest(quest)
            .title("Level 1")
            .orderIndex(1)
            .content("Solve it")
            .requiredMainCodesCount(2)
            .timeoutSeconds(1800)
            .build();
    levelProgress =
        LevelProgress.builder()
            .id(30L)
            .questProgress(questProgress)
            .level(level)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(opened)
            .autoTransitionAt(autoAt)
            .build();
  }

  @Test
  void getCurrentLevel_returnsAggregatedView_forTeamMember() {
    when(userService.getCurrentUser(authentication)).thenReturn(player);
    when(questProgressRepository.findByQuestIdAndTeamId(100L, 10L))
        .thenReturn(Optional.of(questProgress));
    when(teamMemberRepository.findByUserAndTeam(player, team))
        .thenReturn(Optional.of(TeamMember.builder().id(1L).user(player).team(team).build()));
    when(levelProgressRepository.findByQuestProgressIdAndStatus(50L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    when(codeSubmissionRepository.countDistinctSolvedCodeIndexes(30L)).thenReturn(1L);
    List<HintProgressResponse> hints = List.of();
    when(hintProgressService.getVisibleHints(100L, 10L, authentication)).thenReturn(hints);

    CurrentLevelResponse response =
        currentLevelService.getCurrentLevel(100L, 10L, authentication);

    assertThat(response.getLevelProgressId()).isEqualTo(30L);
    assertThat(response.getLevelProgressStatus()).isEqualTo(LevelProgressStatus.ACTIVE);
    assertThat(response.getOpenedAt()).isEqualTo(opened);
    assertThat(response.getAutoTransitionAt()).isEqualTo(autoAt);
    assertThat(response.getLevelId()).isEqualTo(20L);
    assertThat(response.getOrderIndex()).isEqualTo(1);
    assertThat(response.getTitle()).isEqualTo("Level 1");
    assertThat(response.getContent()).isEqualTo("Solve it");
    assertThat(response.getRequiredMainCodesCount()).isEqualTo(2);
    assertThat(response.getTimeoutSeconds()).isEqualTo(1800);
    assertThat(response.getMainCodesSolved()).isEqualTo(1L);
    assertThat(response.getHints()).isEqualTo(hints);
    verify(hintProgressService).getVisibleHints(100L, 10L, authentication);
  }

  @Test
  void getCurrentLevel_allowsAdminWithoutMembership() {
    when(userService.getCurrentUser(authentication)).thenReturn(admin);
    when(questProgressRepository.findByQuestIdAndTeamId(100L, 10L))
        .thenReturn(Optional.of(questProgress));
    when(levelProgressRepository.findByQuestProgressIdAndStatus(50L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));
    when(codeSubmissionRepository.countDistinctSolvedCodeIndexes(30L)).thenReturn(0L);
    when(hintProgressService.getVisibleHints(100L, 10L, authentication)).thenReturn(List.of());

    CurrentLevelResponse response =
        currentLevelService.getCurrentLevel(100L, 10L, authentication);

    assertThat(response.getLevelId()).isEqualTo(20L);
    assertThat(response.getMainCodesSolved()).isEqualTo(0L);
  }

  @Test
  void getCurrentLevel_throwsNotFound_whenNoQuestProgress() {
    when(userService.getCurrentUser(authentication)).thenReturn(player);
    when(questProgressRepository.findByQuestIdAndTeamId(100L, 10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> currentLevelService.getCurrentLevel(100L, 10L, authentication))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Прогресс команды");
  }

  @Test
  void getCurrentLevel_throwsForbidden_whenNotMemberAndNotAdmin() {
    when(userService.getCurrentUser(authentication)).thenReturn(outsider);
    when(questProgressRepository.findByQuestIdAndTeamId(100L, 10L))
        .thenReturn(Optional.of(questProgress));
    when(teamMemberRepository.findByUserAndTeam(outsider, team)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> currentLevelService.getCurrentLevel(100L, 10L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("участник команды");
  }

  @Test
  void getCurrentLevel_throwsNotFound_whenNoActiveLevel() {
    when(userService.getCurrentUser(authentication)).thenReturn(player);
    when(questProgressRepository.findByQuestIdAndTeamId(100L, 10L))
        .thenReturn(Optional.of(questProgress));
    when(teamMemberRepository.findByUserAndTeam(player, team))
        .thenReturn(Optional.of(TeamMember.builder().id(1L).user(player).team(team).build()));
    when(levelProgressRepository.findByQuestProgressIdAndStatus(50L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> currentLevelService.getCurrentLevel(100L, 10L, authentication))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("нет активного уровня");
  }
}
