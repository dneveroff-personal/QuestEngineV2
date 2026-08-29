package dn.questenginev2.level.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.code.repository.CodeSubmissionRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.repository.LevelProgressRepository;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestAuthor;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.quest.repository.QuestRegistrationRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.team.repository.TeamJoinRequestRepository;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.repository.UserRepository;
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
class LevelControllerIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private QuestRepository questRepository;

  @Autowired private QuestAuthorRepository questAuthorRepository;

  @Autowired private LevelRepository levelRepository;

  @Autowired private HintRepository hintRepository;

  @Autowired private CodeRepository codeRepository;

  @Autowired private CodeSubmissionRepository codeSubmissionRepository;

  @Autowired private LevelProgressRepository levelProgressRepository;

  @Autowired private QuestProgressRepository questProgressRepository;

  @Autowired private QuestRegistrationRepository questRegistrationRepository;

  @Autowired private TeamRepository teamRepository;

  @Autowired private TeamMemberRepository teamMemberRepository;

  @Autowired private TeamJoinRequestRepository teamJoinRequestRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private User authorUser;
  private String authorToken;

  @BeforeEach
  void setUp() throws Exception {
    codeSubmissionRepository.deleteAll();
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
  void createLevel_returnsCreatedLevel_whenUserIsAuthorized() throws Exception {
    // Create a quest first
    Quest quest =
        Quest.builder()
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .build();
    quest = questRepository.save(quest);

    // Create QuestAuthor record so the author can create levels
    QuestAuthor questAuthor = QuestAuthor.builder().quest(quest).user(authorUser).build();
    questAuthorRepository.save(questAuthor);

    mockMvc
        .perform(
            post("/api/quests/" + quest.getId() + "/levels")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Level 1\",\"content\":\"Level content\",\"timeoutSeconds\":300}"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.questId").value(quest.getId()))
        .andExpect(jsonPath("$.title").value("Level 1"))
        .andExpect(jsonPath("$.orderIndex").value(1))
        .andExpect(jsonPath("$.content").value("Level content"))
        .andExpect(jsonPath("$.timeoutSeconds").value(300));
  }
}
