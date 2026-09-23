package dn.questenginev2.code.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.auth.repository.RefreshTokenRepository;
import dn.questenginev2.bonuspenalty.repository.ManualTimeAdjustmentRepository;
import dn.questenginev2.code.entity.Code;
import dn.questenginev2.code.entity.CodeSubmission;
import dn.questenginev2.code.entity.CodeSubmissionResult;
import dn.questenginev2.code.entity.CodeType;
import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.code.repository.CodeSubmissionRepository;
import dn.questenginev2.hint.repository.HintProgressRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.repository.QuestRegistrationRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.security.LoginRateLimitFilter;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.entity.TeamRole;
import dn.questenginev2.team.repository.TeamJoinRequestRepository;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Network-retry contract for CodeSubmission (backlog #6, code-submission.md). */
@SpringBootTest
@AutoConfigureMockMvc
class CodeSubmissionRetryIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private QuestRepository questRepository;
  @Autowired private LevelRepository levelRepository;
  @Autowired private CodeRepository codeRepository;
  @Autowired private CodeSubmissionRepository codeSubmissionRepository;
  @Autowired private HintProgressRepository hintProgressRepository;
  @Autowired private ManualTimeAdjustmentRepository manualTimeAdjustmentRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private QuestProgressRepository questProgressRepository;
  @Autowired private LevelProgressRepository levelProgressRepository;
  @Autowired private HintRepository hintRepository;
  @Autowired private QuestAuthorRepository questAuthorRepository;
  @Autowired private QuestRegistrationRepository questRegistrationRepository;
  @Autowired private TeamJoinRequestRepository teamJoinRequestRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private LoginRateLimitFilter loginRateLimitFilter;

  private Quest quest;
  private Team team;
  private LevelProgress levelProgress;
  private User teamMemberUser;
  private String teamMemberToken;

  @BeforeEach
  void setUp() throws Exception {
    loginRateLimitFilter.clear();
    codeSubmissionRepository.deleteAll();
    hintProgressRepository.deleteAll();
    levelProgressRepository.deleteAll();
    manualTimeAdjustmentRepository.deleteAll();
    questProgressRepository.deleteAll();
    codeRepository.deleteAll();
    hintRepository.deleteAll();
    levelRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamRepository.deleteAll();
    questRegistrationRepository.deleteAll();
    questAuthorRepository.deleteAll();
    questRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();

    teamMemberUser = new User();
    teamMemberUser.setUsername("player1");
    teamMemberUser.setPublicName("Player One");
    teamMemberUser.setEmail("player1@example.com");
    teamMemberUser.setPasswordHash(passwordEncoder.encode("password123"));
    teamMemberUser.setRole(UserRole.PLAYER);
    teamMemberUser = userRepository.save(teamMemberUser);

    String response =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"player1\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    teamMemberToken = response.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");

    team = teamRepository.save(Team.builder().name("Team A").captain(teamMemberUser).build());
    teamMemberRepository.save(
        TeamMember.builder()
            .team(team)
            .user(teamMemberUser)
            .role(TeamRole.CAPTAIN)
            .joinedAt(Instant.now())
            .build());

    quest =
        questRepository.save(
            Quest.builder()
                .title("Test Quest")
                .description("Test Description")
                .type(QuestType.TEAM)
                .status(QuestStatus.RUNNING)
                .build());
  }

  private LevelProgress setUpActiveLevelWithCodes(Integer requiredMainCodesCount, Code... codes) {
    Level level =
        levelRepository.save(
            Level.builder()
                .quest(quest)
                .title("L1")
                .orderIndex(1)
                .timeoutSeconds(600)
                .requiredMainCodesCount(requiredMainCodesCount)
                .build());
    for (Code code : codes) {
      code.setLevel(level);
      codeRepository.save(code);
    }
    QuestProgress questProgress =
        questProgressRepository.save(
            QuestProgress.builder()
                .quest(quest)
                .team(team)
                .status(QuestProgressStatus.RUNNING)
                .questStartedAt(Instant.now())
                .build());
    return levelProgressRepository.save(
        LevelProgress.builder()
            .questProgress(questProgress)
            .level(level)
            .status(LevelProgressStatus.ACTIVE)
            .openedAt(Instant.now())
            .build());
  }

  @Test
  void submitCode_retrySameMainOnActiveLevel_doesNotDoubleComplete() throws Exception {
    levelProgress =
        setUpActiveLevelWithCodes(
            2,
            Code.builder().value("code-a").type(CodeType.MAIN).codeIndex(1).build(),
            Code.builder().value("code-b").type(CodeType.MAIN).codeIndex(2).build());

    mockMvc
        .perform(
            post("/api/quests/progress/" + quest.getId() + "/" + team.getId() + "/codes")
                .header("Authorization", "Bearer " + teamMemberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"code-a\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("CORRECT_MAIN"))
        .andExpect(jsonPath("$.levelCompleted").value(false))
        .andExpect(jsonPath("$.remainingMainCodes").value(1));

    mockMvc
        .perform(
            post("/api/quests/progress/" + quest.getId() + "/" + team.getId() + "/codes")
                .header("Authorization", "Bearer " + teamMemberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"code-a\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("CORRECT_MAIN"))
        .andExpect(jsonPath("$.levelCompleted").value(false))
        .andExpect(jsonPath("$.remainingMainCodes").value(1));

    List<CodeSubmission> submissions =
        codeSubmissionRepository.findByLevelProgressIdOrderBySubmittedAtDesc(levelProgress.getId());
    long mainHits =
        submissions.stream()
            .filter(s -> s.getResult() == CodeSubmissionResult.CORRECT_MAIN)
            .count();
    assertThat(mainHits).isEqualTo(2);
    assertThat(levelProgressRepository.findById(levelProgress.getId()).orElseThrow().getStatus())
        .isEqualTo(LevelProgressStatus.ACTIVE);
  }

  @Test
  void submitCode_retryAfterLevelCompleted_returnsConflictNoActiveLevel() throws Exception {
    levelProgress =
        setUpActiveLevelWithCodes(
            1, Code.builder().value("only").type(CodeType.MAIN).codeIndex(1).build());

    mockMvc
        .perform(
            post("/api/quests/progress/" + quest.getId() + "/" + team.getId() + "/codes")
                .header("Authorization", "Bearer " + teamMemberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"only\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("CORRECT_MAIN"))
        .andExpect(jsonPath("$.levelCompleted").value(true));

    mockMvc
        .perform(
            post("/api/quests/progress/" + quest.getId() + "/" + team.getId() + "/codes")
                .header("Authorization", "Bearer " + teamMemberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"only\"}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
