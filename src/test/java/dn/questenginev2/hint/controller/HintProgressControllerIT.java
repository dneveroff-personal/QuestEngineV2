package dn.questenginev2.hint.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.code.repository.CodeSubmissionRepository;
import dn.questenginev2.hint.entity.Hint;
import dn.questenginev2.hint.entity.HintType;
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
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.scheduling.HintRevealScheduler;
import dn.questenginev2.team.entity.Team;
import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.team.entity.TeamRole;
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

/**
 * Сквозной тест видимости подсказок (ADR-0020, ADR-0021): REGULAR auto-reveal через Job 3,
 * BONUS/PENALTY доступны, но требуют явного взятия через {@code POST .../hints/{hintId}/take}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HintProgressControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private QuestRepository questRepository;
  @Autowired private LevelRepository levelRepository;
  @Autowired private HintRepository hintRepository;
  @Autowired private HintProgressRepository hintProgressRepository;
  @Autowired private CodeRepository codeRepository;
  @Autowired private CodeSubmissionRepository codeSubmissionRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private QuestProgressRepository questProgressRepository;
  @Autowired private LevelProgressRepository levelProgressRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private HintRevealScheduler hintRevealScheduler;

  private Quest quest;
  private Team team;
  private LevelProgress levelProgress;
  private Hint regularHint;
  private Hint penaltyHint;
  private Hint futureBonusHint;
  private String teamMemberToken;

  @BeforeEach
  void setUp() throws Exception {
    hintProgressRepository.deleteAll();
    codeSubmissionRepository.deleteAll();
    levelProgressRepository.deleteAll();
    questProgressRepository.deleteAll();
    codeRepository.deleteAll();
    hintRepository.deleteAll();
    levelRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamRepository.deleteAll();
    questRepository.deleteAll();
    userRepository.deleteAll();

    User teamMemberUser = new User();
    teamMemberUser.setUsername("hintplayer1");
    teamMemberUser.setPublicName("Hint Player One");
    teamMemberUser.setEmail("hintplayer1@example.com");
    teamMemberUser.setPasswordHash(passwordEncoder.encode("password123"));
    teamMemberUser.setRole(UserRole.PLAYER);
    teamMemberUser = userRepository.save(teamMemberUser);

    String response =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"hintplayer1\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    teamMemberToken = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

    team = teamRepository.save(Team.builder().name("Hint Team").captain(teamMemberUser).build());
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
                .title("Hint Quest")
                .description("Test Description")
                .type(QuestType.TEAM)
                .status(QuestStatus.RUNNING)
                .build());

    Level level =
        levelRepository.save(
            Level.builder().quest(quest).title("L1").orderIndex(1).timeoutSeconds(3600).build());

    // REGULAR, задержка истекла — auto-reveal через Job 3.
    regularHint =
        hintRepository.save(
            Hint.builder()
                .level(level)
                .orderIndex(1)
                .delaySeconds(60)
                .content("Regular hint: check the map")
                .type(HintType.REGULAR)
                .build());

    // PENALTY, задержка истекла — доступна, но требует явного взятия (ADR-0021).
    penaltyHint =
        hintRepository.save(
            Hint.builder()
                .level(level)
                .orderIndex(2)
                .delaySeconds(60)
                .content("The code is written on the wall")
                .type(HintType.PENALTY)
                .bonusPenaltySeconds(600)
                .build());

    // BONUS, задержка ещё не истекла — не должна быть видна вообще.
    futureBonusHint =
        hintRepository.save(
            Hint.builder()
                .level(level)
                .orderIndex(3)
                .delaySeconds(3600)
                .content("Bonus hint: much later")
                .type(HintType.BONUS)
                .bonusPenaltySeconds(120)
                .build());

    QuestProgress questProgress =
        questProgressRepository.save(
            QuestProgress.builder()
                .quest(quest)
                .team(team)
                .status(QuestProgressStatus.RUNNING)
                .questStartedAt(Instant.now())
                .build());

    levelProgress =
        levelProgressRepository.save(
            LevelProgress.builder()
                .questProgress(questProgress)
                .level(level)
                .status(LevelProgressStatus.ACTIVE)
                .openedAt(Instant.now().minusSeconds(120))
                .build());
  }

  @Test
  void getVisibleHints_returnsPenaltyTypeOnly_beforeSchedulerRuns() throws Exception {
    // Job 3 ещё не запускался: REGULAR (истёкшая задержка) не показана — не включаем в ответ
    // (ADR-0020: content не палим до момента показа). PENALTY (истёкшая задержка) — доступна, но не
    // взята: виден только тип, content/bonusPenaltySeconds отсутствуют (ADR-0021). BONUS с
    // неистёкшей задержкой — не виден вообще.
    mockMvc
        .perform(
            get("/api/quests/progress/" + quest.getId() + "/" + team.getId() + "/hints")
                .header("Authorization", "Bearer " + teamMemberToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].type").value("PENALTY"))
        .andExpect(jsonPath("$[?(@.type=='REGULAR')]").doesNotExist())
        .andExpect(jsonPath("$[?(@.type=='BONUS')]").doesNotExist());
  }

  @Test
  void getVisibleHints_showsRegularAutoRevealed_andPenaltyTypeOnly_afterSchedulerRuns()
      throws Exception {
    hintRevealScheduler.revealDueHints();

    mockMvc
        .perform(
            get("/api/quests/progress/" + quest.getId() + "/" + team.getId() + "/hints")
                .header("Authorization", "Bearer " + teamMemberToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        // REGULAR — auto-revealed, full content.
        .andExpect(jsonPath("$[?(@.type=='REGULAR')].content").value("Regular hint: check the map"))
        // PENALTY — available but not taken: type visible, content/cost hidden.
        .andExpect(jsonPath("$[?(@.type=='PENALTY')].content").isEmpty())
        .andExpect(jsonPath("$[?(@.type=='PENALTY')].bonusPenaltySeconds").isEmpty());

    // BONUS с неистёкшей задержкой не должен быть создан Job 3 (он его вообще не трогает).
    assertThat(
            hintProgressRepository.existsByLevelProgressIdAndHintId(
                levelProgress.getId(), futureBonusHint.getId()))
        .isFalse();
    // PENALTY не должен быть авто-показан Job 3 — только REGULAR.
    assertThat(
            hintProgressRepository.existsByLevelProgressIdAndHintId(
                levelProgress.getId(), penaltyHint.getId()))
        .isFalse();
  }

  @Test
  void takeHint_revealsContent_andPersistsHintProgress() throws Exception {
    hintRevealScheduler.revealDueHints(); // REGULAR auto-revealed, PENALTY остаётся нетронутым

    mockMvc
        .perform(
            post("/api/quests/progress/"
                    + quest.getId()
                    + "/"
                    + team.getId()
                    + "/hints/"
                    + penaltyHint.getId()
                    + "/take")
                .header("Authorization", "Bearer " + teamMemberToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("The code is written on the wall"))
        .andExpect(jsonPath("$.bonusPenaltySeconds").value(600));

    assertThat(
            hintProgressRepository.existsByLevelProgressIdAndHintId(
                levelProgress.getId(), penaltyHint.getId()))
        .isTrue();

    // После взятия GET должен возвращать полный content для этой подсказки.
    mockMvc
        .perform(
            get("/api/quests/progress/" + quest.getId() + "/" + team.getId() + "/hints")
                .header("Authorization", "Bearer " + teamMemberToken))
        .andExpect(
            jsonPath("$[?(@.type=='PENALTY')].content").value("The code is written on the wall"));
  }

  @Test
  void takeHint_returnsConflict_whenHintNotYetAvailable() throws Exception {
    mockMvc
        .perform(
            post("/api/quests/progress/"
                    + quest.getId()
                    + "/"
                    + team.getId()
                    + "/hints/"
                    + futureBonusHint.getId()
                    + "/take")
                .header("Authorization", "Bearer " + teamMemberToken))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void takeHint_isIdempotent_whenCalledTwice() throws Exception {
    String path =
        "/api/quests/progress/"
            + quest.getId()
            + "/"
            + team.getId()
            + "/hints/"
            + penaltyHint.getId()
            + "/take";

    mockMvc
        .perform(post(path).header("Authorization", "Bearer " + teamMemberToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(post(path).header("Authorization", "Bearer " + teamMemberToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("The code is written on the wall"));

    assertThat(hintProgressRepository.findByLevelProgressIdOrderByShownAt(levelProgress.getId()))
        .hasSize(1);
  }

  @Test
  void getVisibleHints_returnsConflict_whenUserNotTeamMember() throws Exception {
    User outsider = new User();
    outsider.setUsername("hintoutsider");
    outsider.setPublicName("Outsider");
    outsider.setEmail("hintoutsider@example.com");
    outsider.setPasswordHash(passwordEncoder.encode("password123"));
    outsider.setRole(UserRole.PLAYER);
    userRepository.save(outsider);

    String outsiderResponse =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"hintoutsider\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String outsiderToken = outsiderResponse.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

    mockMvc
        .perform(
            get("/api/quests/progress/" + quest.getId() + "/" + team.getId() + "/hints")
                .header("Authorization", "Bearer " + outsiderToken))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
