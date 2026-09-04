package dn.questenginev2.hint.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.hint.dto.HintProgressResponse;
import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.hint.entity.HintProgress;
import dn.questenginev2.hint.entity.HintType;
import dn.questenginev2.hint.repository.HintProgressRepository;
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
class HintProgressServiceImplTest {

  @Mock private HintProgressRepository hintProgressRepository;
  @Mock private QuestProgressRepository questProgressRepository;
  @Mock private LevelProgressRepository levelProgressRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private UserService userService;
  @Mock private Authentication authentication;

  @InjectMocks private HintProgressServiceImpl hintProgressService;

  private User currentUser;
  private Team team;
  private QuestProgress questProgress;
  private Level level;
  private LevelProgress levelProgress;

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
            .openedAt(Instant.now())
            .build();

    when(userService.getCurrentUser(authentication)).thenReturn(currentUser);
    when(questProgressRepository.findByQuestIdAndTeamId(100L, 10L))
        .thenReturn(Optional.of(questProgress));
    lenient()
        .when(teamMemberRepository.findByUserAndTeam(currentUser, team))
        .thenReturn(Optional.of(TeamMember.builder().id(1L).user(currentUser).team(team).build()));
  }

  @Test
  void getShownHints_returnsEmptyList_whenNoActiveLevel() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.empty());

    List<HintProgressResponse> response =
        hintProgressService.getShownHints(100L, 10L, authentication);

    assertThat(response).isEmpty();
  }

  @Test
  void getShownHints_returnsShownHints_whenActiveLevelHasHintProgress() {
    when(levelProgressRepository.findByQuestProgressIdAndStatus(500L, LevelProgressStatus.ACTIVE))
        .thenReturn(Optional.of(levelProgress));

    Hint hint =
        Hint.builder()
            .id(1L)
            .level(level)
            .orderIndex(1)
            .delaySeconds(60)
            .content("Look under the bridge")
            .type(HintType.REGULAR)
            .build();
    HintProgress hintProgress =
        HintProgress.builder()
            .id(1L)
            .levelProgress(levelProgress)
            .hint(hint)
            .shownAt(Instant.now())
            .build();
    when(hintProgressRepository.findByLevelProgressIdOrderByShownAt(2000L))
        .thenReturn(List.of(hintProgress));

    List<HintProgressResponse> response =
        hintProgressService.getShownHints(100L, 10L, authentication);

    assertThat(response).hasSize(1);
    assertThat(response.get(0).getContent()).isEqualTo("Look under the bridge");
    assertThat(response.get(0).getType()).isEqualTo(HintType.REGULAR);
  }

  @Test
  void getShownHints_throwsForbiddenOperationException_whenUserNotTeamMember() {
    when(teamMemberRepository.findByUserAndTeam(currentUser, team)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hintProgressService.getShownHints(100L, 10L, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("участник");
  }

  @Test
  void getShownHints_throwsIllegalArgumentException_whenQuestProgressNotFound() {
    when(questProgressRepository.findByQuestIdAndTeamId(100L, 10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hintProgressService.getShownHints(100L, 10L, authentication))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
