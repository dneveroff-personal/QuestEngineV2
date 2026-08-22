package dn.questenginev2.quest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestAuthor;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.entity.QuestProgressStatus;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.team.entity.Team;
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
class QuestControllerIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private QuestRepository questRepository;

  @Autowired private QuestAuthorRepository questAuthorRepository;

  @Autowired private QuestProgressRepository questProgressRepository;

  @Autowired private LevelRepository levelRepository;

  @Autowired private HintRepository hintRepository;

  @Autowired private CodeRepository codeRepository;

  @Autowired private TeamRepository teamRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private User authorUser;
  private String authorToken;

  @BeforeEach
  void setUp() throws Exception {
    questProgressRepository.deleteAll();
    codeRepository.deleteAll();
    hintRepository.deleteAll();
    levelRepository.deleteAll();
    questAuthorRepository.deleteAll();
    questRepository.deleteAll();
    teamRepository.deleteAll();
    userRepository.deleteAll();

    // Create an author user directly with properly encoded password
    authorUser = new User();
    authorUser.setUsername("author");
    authorUser.setPublicName("Author User");
    authorUser.setEmail("author@example.com");
    authorUser.setPasswordHash(passwordEncoder.encode("password123"));
    authorUser.setRole(UserRole.AUTHOR);
    authorUser = userRepository.save(authorUser);

    // Get JWT token for author
    String response =
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"author\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    authorToken = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
  }

  @Test
  void createQuest_returnsCreatedQuest_whenUserIsAuthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/quests")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Test Quest\",\"description\":\"Test"
                        + " Description\",\"type\":\"TEAM\"}"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.title").value("Test Quest"))
        .andExpect(jsonPath("$.description").value("Test Description"))
        .andExpect(jsonPath("$.type").value("TEAM"))
        .andExpect(jsonPath("$.status").value("DRAFT"));
  }

  @Test
  void getQuestById_returnsQuest_whenQuestExists() throws Exception {
    // Create a quest first
    Quest quest =
        Quest.builder()
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .build();
    quest = questRepository.save(quest);

    // Create QuestAuthor record
    QuestAuthor questAuthor = QuestAuthor.builder().quest(quest).user(authorUser).build();
    questAuthorRepository.save(questAuthor);

    mockMvc
        .perform(
            get("/api/quests/" + quest.getId()).header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(quest.getId()))
        .andExpect(jsonPath("$.title").value("Test Quest"));
  }

  @Test
  void updateQuest_returnsUpdatedQuest_whenUserIsAuthorized() throws Exception {
    // Create a quest first
    Quest quest =
        Quest.builder()
            .title("Old Title")
            .description("Old Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .build();
    quest = questRepository.save(quest);

    // Create QuestAuthor record
    QuestAuthor questAuthor = QuestAuthor.builder().quest(quest).user(authorUser).build();
    questAuthorRepository.save(questAuthor);

    mockMvc
        .perform(
            put("/api/quests/" + quest.getId())
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"New Title\",\"description\":\"New"
                        + " Description\",\"type\":\"SINGLE\"}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.title").value("New Title"))
        .andExpect(jsonPath("$.type").value("SINGLE"));
  }

  @Test
  void deleteQuest_returnsNoContent_whenQuestExists() throws Exception {
    // Create a quest first
    Quest quest =
        Quest.builder()
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .build();
    quest = questRepository.save(quest);

    // Create QuestAuthor record so the author can delete the quest
    QuestAuthor questAuthor = QuestAuthor.builder().quest(quest).user(authorUser).build();
    questAuthorRepository.save(questAuthor);

    mockMvc
        .perform(
            delete("/api/quests/" + quest.getId()).header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isOk());
  }

  @Test
  void publishQuest_returnsRegistrationStatus_whenDraftHasValidLevel() throws Exception {
    Quest quest =
        Quest.builder()
            .title("Publishable Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .build();
    quest = questRepository.save(quest);
    questAuthorRepository.save(QuestAuthor.builder().quest(quest).user(authorUser).build());

    Level level =
        Level.builder().quest(quest).title("L1").orderIndex(1).timeoutSeconds(600).build();
    levelRepository.save(level);

    mockMvc
        .perform(
            post("/api/quests/" + quest.getId() + "/publish")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value("REGISTRATION"));
  }

  @Test
  void publishQuest_returnsConflict_whenQuestHasNoLevels() throws Exception {
    Quest quest =
        Quest.builder()
            .title("Empty Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .build();
    quest = questRepository.save(quest);
    questAuthorRepository.save(QuestAuthor.builder().quest(quest).user(authorUser).build());

    mockMvc
        .perform(
            post("/api/quests/" + quest.getId() + "/publish")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void publishQuest_returnsConflict_whenLevelIsAnomalous() throws Exception {
    Quest quest =
        Quest.builder()
            .title("Broken Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .build();
    quest = questRepository.save(quest);
    questAuthorRepository.save(QuestAuthor.builder().quest(quest).user(authorUser).build());

    // Level with no timeout and no codes — anomalous per ADR-0005
    Level level = Level.builder().quest(quest).title("Anomalous Level").orderIndex(1).build();
    levelRepository.save(level);

    mockMvc
        .perform(
            post("/api/quests/" + quest.getId() + "/publish")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("непроходим")));
  }

  @Test
  void finishQuest_returnsFinishedStatus_andMarksUnfinishedTeamsAsDnf() throws Exception {
    Quest quest =
        Quest.builder()
            .title("Running Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.RUNNING)
            .build();
    quest = questRepository.save(quest);
    questAuthorRepository.save(QuestAuthor.builder().quest(quest).user(authorUser).build());

    Team team = teamRepository.save(Team.builder().name("Team A").captain(authorUser).build());
    QuestProgress progress =
        QuestProgress.builder()
            .quest(quest)
            .team(team)
            .status(QuestProgressStatus.RUNNING)
            .questStartedAt(Instant.now())
            .build();
    progress = questProgressRepository.save(progress);

    mockMvc
        .perform(
            post("/api/quests/" + quest.getId() + "/finish")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value("FINISHED"));

    QuestProgress reloaded = questProgressRepository.findById(progress.getId()).orElseThrow();
    org.assertj.core.api.Assertions.assertThat(reloaded.getStatus())
        .isEqualTo(QuestProgressStatus.DNF);
  }

  @Test
  void finishQuest_returnsConflict_whenQuestNotRunning() throws Exception {
    Quest quest =
        Quest.builder()
            .title("Draft Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .build();
    quest = questRepository.save(quest);
    questAuthorRepository.save(QuestAuthor.builder().quest(quest).user(authorUser).build());

    mockMvc
        .perform(
            post("/api/quests/" + quest.getId() + "/finish")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
