package dn.questenginev2.bonuspenalty.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.auth.repository.RefreshTokenRepository;
import dn.questenginev2.bonuspenalty.repository.ManualTimeAdjustmentRepository;
import dn.questenginev2.code.entity.Code;
import dn.questenginev2.code.entity.CodeSubmission;
import dn.questenginev2.code.entity.CodeSubmissionResult;
import dn.questenginev2.code.entity.CodeType;
import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.code.repository.CodeSubmissionRepository;
import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.hint.entity.HintProgress;
import dn.questenginev2.hint.entity.HintType;
import dn.questenginev2.hint.repository.HintProgressRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.level.entity.LevelProgressStatus;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestAuthor;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ManualTimeAdjustmentControllerIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private QuestRepository questRepository;
  @Autowired private QuestAuthorRepository questAuthorRepository;
  @Autowired private QuestProgressRepository questProgressRepository;
  @Autowired private LevelRepository levelRepository;
  @Autowired private LevelProgressRepository levelProgressRepository;
  @Autowired private CodeRepository codeRepository;
  @Autowired private CodeSubmissionRepository codeSubmissionRepository;
  @Autowired private HintRepository hintRepository;
  @Autowired private HintProgressRepository hintProgressRepository;
  @Autowired private ManualTimeAdjustmentRepository manualTimeAdjustmentRepository;
  @Autowired private QuestRegistrationRepository questRegistrationRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private TeamJoinRequestRepository teamJoinRequestRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private LoginRateLimitFilter loginRateLimitFilter;

  private User authorUser;
  private String authorToken;
  private User playerUser;
  private String playerToken;
  private Quest quest;
  private Team team;
  private QuestProgress questProgress;
  private LevelProgress levelProgress;

  @BeforeEach
  void setUp() throws Exception {
    loginRateLimitFilter.clear();
    codeSubmissionRepository.deleteAll();
    hintProgressRepository.deleteAll();
    manualTimeAdjustmentRepository.deleteAll();
    levelProgressRepository.deleteAll();
    questProgressRepository.deleteAll();
    codeRepository.deleteAll();
    hintRepository.deleteAll();
    levelRepository.deleteAll();
    questRegistrationRepository.deleteAll();
    questAuthorRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamJoinRequestRepository.deleteAll();
    teamRepository.deleteAll();
    questRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();

    // Create author user
    authorUser = new User();
    authorUser.setUsername("author");
    authorUser.setPublicName("Author User");
    authorUser.setEmail("author@example.com");
    authorUser.setPasswordHash(passwordEncoder.encode("password123"));
    authorUser.setRole(UserRole.AUTHOR);
    authorUser = userRepository.save(authorUser);

    // Create player user
    playerUser = new User();
    playerUser.setUsername("player1");
    playerUser.setPublicName("Player One");
    playerUser.setEmail("player1@example.com");
    playerUser.setPasswordHash(passwordEncoder.encode("password123"));
    playerUser.setRole(UserRole.PLAYER);
    playerUser = userRepository.save(playerUser);

    // Get JWT tokens
    String authorResponse =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"author\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    authorToken = authorResponse.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");

    String playerResponse =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"player1\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    playerToken = playerResponse.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");

    // Create team with player as captain
    team = teamRepository.save(Team.builder().name("Team A").captain(playerUser).build());
    teamMemberRepository.save(
        TeamMember.builder()
            .team(team)
            .user(playerUser)
            .role(TeamRole.CAPTAIN)
            .joinedAt(Instant.now())
            .build());

    // Create quest in RUNNING status
    quest =
        questRepository.save(
            Quest.builder()
                .title("Test Quest")
                .description("Test Description")
                .type(QuestType.TEAM)
                .status(QuestStatus.RUNNING)
                .build());
    questAuthorRepository.save(QuestAuthor.builder().quest(quest).user(authorUser).build());

    // Create level
    Level level =
        levelRepository.save(
            Level.builder().quest(quest).title("L1").orderIndex(1).timeoutSeconds(600).build());

    // Create quest progress for the team
    questProgress =
        questProgressRepository.save(
            QuestProgress.builder()
                .quest(quest)
                .team(team)
                .status(QuestProgressStatus.RUNNING)
                .questStartedAt(Instant.now())
                .build());

    // Create active level progress
    levelProgress =
        levelProgressRepository.save(
            LevelProgress.builder()
                .questProgress(questProgress)
                .level(level)
                .status(LevelProgressStatus.ACTIVE)
                .openedAt(Instant.now())
                .build());
  }

  @Test
  void createAdjustment_returnsCreated_whenAuthorCreatesBonus() throws Exception {
    String requestBody = "{\"type\":\"BONUS\",\"seconds\":300,\"reason\":\"Great teamwork\"}";

    mockMvc
        .perform(
            post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("BONUS"))
        .andExpect(jsonPath("$.seconds").value(300))
        .andExpect(jsonPath("$.reason").value("Great teamwork"))
        .andExpect(jsonPath("$.createdByUserId").value(authorUser.getId()))
        .andExpect(jsonPath("$.revokedAt").doesNotExist());
  }

  @Test
  void createAdjustment_returnsCreated_whenAuthorCreatesPenalty() throws Exception {
    String requestBody = "{\"type\":\"PENALTY\",\"seconds\":600,\"reason\":\"Rule violation\"}";

    mockMvc
        .perform(
            post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("PENALTY"))
        .andExpect(jsonPath("$.seconds").value(600))
        .andExpect(jsonPath("$.reason").value("Rule violation"));
  }

  @Test
  void createAdjustment_returnsForbidden_whenPlayerTriesToCreate() throws Exception {
    String requestBody = "{\"type\":\"BONUS\",\"seconds\":300,\"reason\":\"Cheating\"}";

    mockMvc
        .perform(
            post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  void createAdjustment_returnsBadRequest_whenSecondsNotPositive() throws Exception {
    String requestBody = "{\"type\":\"BONUS\",\"seconds\":0,\"reason\":\"Invalid\"}";

    mockMvc
        .perform(
            post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createAdjustment_returnsBadRequest_whenReasonBlank() throws Exception {
    String requestBody = "{\"type\":\"BONUS\",\"seconds\":300,\"reason\":\"\"}";

    mockMvc
        .perform(
            post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getAdjustments_returnsList_whenAdjustmentsExist() throws Exception {
    // Create two adjustments
    mockMvc
        .perform(
            post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BONUS\",\"seconds\":300,\"reason\":\"Bonus 1\"}"))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"PENALTY\",\"seconds\":200,\"reason\":\"Penalty 1\"}"))
        .andExpect(status().isCreated());

    // Get adjustments
    mockMvc
        .perform(
            get("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].type").value("BONUS"))
        .andExpect(jsonPath("$[1].type").value("PENALTY"));
  }

  @Test
  void getAdjustments_includesRevokedEntries() throws Exception {
    // Create adjustment
    String createResponse =
        mockMvc
            .perform(
                post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                    .header("Authorization", "Bearer " + authorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"BONUS\",\"seconds\":300,\"reason\":\"To be revoked\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long adjustmentId = extractId(createResponse);

    // Revoke it
    mockMvc
        .perform(
            post("/api/adjustments/" + adjustmentId + "/revoke")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revokedAt").exists())
        .andExpect(jsonPath("$.revokedByUserId").value(authorUser.getId()));

    // Get adjustments - should include revoked
    mockMvc
        .perform(
            get("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].revokedAt").exists());
  }

  @Test
  void revokeAdjustment_returnsOk_whenAuthorRevokesBeforeQuestFinished() throws Exception {
    // Create adjustment
    String createResponse =
        mockMvc
            .perform(
                post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                    .header("Authorization", "Bearer " + authorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"BONUS\",\"seconds\":300,\"reason\":\"To revoke\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long adjustmentId = extractId(createResponse);

    // Revoke it
    mockMvc
        .perform(
            post("/api/adjustments/" + adjustmentId + "/revoke")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revokedAt").exists())
        .andExpect(jsonPath("$.revokedByUserId").value(authorUser.getId()));
  }

  @Test
  void revokeAdjustment_returnsConflict_whenQuestAlreadyFinished() throws Exception {
    // Create adjustment
    String createResponse =
        mockMvc
            .perform(
                post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                    .header("Authorization", "Bearer " + authorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"BONUS\",\"seconds\":300,\"reason\":\"To revoke\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long adjustmentId = extractId(createResponse);

    // Finish the quest
    quest.setStatus(QuestStatus.FINISHED);
    questRepository.save(quest);

    // Try to revoke - should fail
    mockMvc
        .perform(
            post("/api/adjustments/" + adjustmentId + "/revoke")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("завершения квеста")));
  }

  @Test
  void revokeAdjustment_returnsConflict_whenAlreadyRevoked() throws Exception {
    // Create adjustment
    String createResponse =
        mockMvc
            .perform(
                post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                    .header("Authorization", "Bearer " + authorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"BONUS\",\"seconds\":300,\"reason\":\"To revoke\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long adjustmentId = extractId(createResponse);

    // Revoke once
    mockMvc
        .perform(
            post("/api/adjustments/" + adjustmentId + "/revoke")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isOk());

    // Revoke again - should fail with Conflict (IllegalArgumentException mapped to 409)
    mockMvc
        .perform(
            post("/api/adjustments/" + adjustmentId + "/revoke")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("уже отозвана")));
  }

  @Test
  void bonusPenaltySeconds_inQuestProgressResponse_reflectsManualAdjustments() throws Exception {
    // Create BONUS adjustment
    mockMvc
        .perform(
            post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BONUS\",\"seconds\":300,\"reason\":\"Bonus\"}"))
        .andExpect(status().isCreated());

    // Create PENALTY adjustment
    mockMvc
        .perform(
            post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"PENALTY\",\"seconds\":200,\"reason\":\"Penalty\"}"))
        .andExpect(status().isCreated());

    // Get quest progress - bonusPenaltySeconds should be penalty - bonus = 200 - 300 = -100
    String progressResponse =
        mockMvc
            .perform(
                get("/api/quests/progress/" + quest.getId() + "/" + team.getId())
                    .header("Authorization", "Bearer " + playerToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // bonusPenaltySeconds = penalty - bonus = 200 - 300 = -100 (negative = net bonus)
    assertThat(progressResponse).contains("\"bonusPenaltySeconds\":-100");
  }

  @Test
  void bonusPenaltySeconds_inQuestProgressResponse_reflectsRevokedAdjustments() throws Exception {
    // Create BONUS adjustment
    String createResponse =
        mockMvc
            .perform(
                post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                    .header("Authorization", "Bearer " + authorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"BONUS\",\"seconds\":300,\"reason\":\"Bonus\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long adjustmentId = extractId(createResponse);

    // Revoke it
    mockMvc
        .perform(
            post("/api/adjustments/" + adjustmentId + "/revoke")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isOk());

    // Get quest progress - bonusPenaltySeconds should be 0 (revoked adjustment not counted)
    String progressResponse =
        mockMvc
            .perform(
                get("/api/quests/progress/" + quest.getId() + "/" + team.getId())
                    .header("Authorization", "Bearer " + playerToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(progressResponse).contains("\"bonusPenaltySeconds\":0");
  }

  @Test
  void bonusPenaltySeconds_aggregatesAllThreeSources() throws Exception {
    // 1. Manual BONUS adjustment: -300 seconds
    mockMvc
        .perform(
            post("/api/quest-progress/" + questProgress.getId() + "/adjustments")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BONUS\",\"seconds\":300,\"reason\":\"Manual bonus\"}"))
        .andExpect(status().isCreated());

    // 2. BONUS code submission: -200 seconds
    Code bonusCode =
        codeRepository.save(
            Code.builder()
                .level(levelProgress.getLevel())
                .value("bonuscode")
                .type(CodeType.BONUS)
                .bonusPenaltySeconds(200)
                .build());
    codeSubmissionRepository.save(
        CodeSubmission.builder()
            .levelProgress(levelProgress)
            .submittedBy(playerUser)
            .rawValue("bonuscode")
            .matchedCode(bonusCode)
            .result(CodeSubmissionResult.CORRECT_BONUS)
            .submittedAt(Instant.now())
            .build());

    // 3. PENALTY hint taken: +150 seconds
    Hint penaltyHint =
        hintRepository.save(
            Hint.builder()
                .level(levelProgress.getLevel())
                .orderIndex(1)
                .delaySeconds(0)
                .content("Penalty hint")
                .type(HintType.PENALTY)
                .bonusPenaltySeconds(150)
                .build());
    hintProgressRepository.save(
        HintProgress.builder()
            .levelProgress(levelProgress)
            .hint(penaltyHint)
            .shownAt(Instant.now())
            .build());

    // Get quest progress
    // Total = manual(-300) + code(-200) + hint(+150) = -350
    // bonusPenaltySeconds = penalty - bonus = 150 - (300+200) = -350
    String progressResponse =
        mockMvc
            .perform(
                get("/api/quests/progress/" + quest.getId() + "/" + team.getId())
                    .header("Authorization", "Bearer " + playerToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(progressResponse).contains("\"bonusPenaltySeconds\":-350");
  }

  private Long extractId(String jsonResponse) {
    // Extract id from JSON response like {"id":123,...}
    return Long.parseLong(jsonResponse.replaceAll(".*\"id\":(\\d+).*", "$1"));
  }
}
