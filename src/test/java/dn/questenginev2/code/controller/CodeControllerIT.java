package dn.questenginev2.code.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.code.entity.Code;
import dn.questenginev2.code.entity.CodeType;
import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.level.entity.Level;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestAuthor;
import dn.questenginev2.quest.entity.QuestStatus;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestRepository;
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
class CodeControllerIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private QuestRepository questRepository;

  @Autowired private QuestAuthorRepository questAuthorRepository;

  @Autowired private LevelRepository levelRepository;

  @Autowired private CodeRepository codeRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private User authorUser;
  private String authorToken;

  @BeforeEach
  void setUp() throws Exception {
    codeRepository.deleteAll();
    levelRepository.deleteAll();
    questAuthorRepository.deleteAll();
    questRepository.deleteAll();
    userRepository.deleteAll();

    authorUser = new User();
    authorUser.setUsername("author");
    authorUser.setPublicName("Author User");
    authorUser.setEmail("author@example.com");
    authorUser.setPasswordHash(passwordEncoder.encode("password123"));
    authorUser.setRole(UserRole.AUTHOR);
    authorUser = userRepository.save(authorUser);

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
  void createCode_returnsCreatedCode_whenUserIsAuthorized() throws Exception {
    Quest quest =
        Quest.builder()
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .build();
    quest = questRepository.save(quest);

    QuestAuthor questAuthor = QuestAuthor.builder().quest(quest).user(authorUser).build();
    questAuthorRepository.save(questAuthor);

    Level level =
        Level.builder()
            .quest(quest)
            .title("Level 1")
            .orderIndex(1)
            .content("Level content")
            .build();
    level = levelRepository.save(level);

    mockMvc
        .perform(
            post("/api/quests/" + quest.getId() + "/levels/" + level.getId() + "/codes")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"value\":\"CODE123\",\"type\":\"MAIN\",\"groupIndex\":1,\"points\":100}"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.levelId").value(level.getId()))
        .andExpect(jsonPath("$.value").value("CODE123"))
        .andExpect(jsonPath("$.type").value("MAIN"))
        .andExpect(jsonPath("$.points").value(100));
  }

  @Test
  void getCodesByLevel_returnsCodes_whenLevelExists() throws Exception {
    Quest quest =
        Quest.builder()
            .title("Test Quest")
            .description("Test Description")
            .type(QuestType.TEAM)
            .status(QuestStatus.DRAFT)
            .build();
    quest = questRepository.save(quest);

    Level level =
        Level.builder()
            .quest(quest)
            .title("Level 1")
            .orderIndex(1)
            .content("Level content")
            .build();
    level = levelRepository.save(level);

    Code code =
        Code.builder().level(level).value("CODE123").type(CodeType.MAIN).points(100).build();
    codeRepository.save(code);

    mockMvc
        .perform(
            get("/api/quests/" + quest.getId() + "/levels/" + level.getId() + "/codes")
                .header("Authorization", "Bearer " + authorToken))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].levelId").value(level.getId()))
        .andExpect(jsonPath("$[0].value").value("CODE123"))
        .andExpect(jsonPath("$[0].type").value("MAIN"))
        .andExpect(jsonPath("$[0].points").value(100));
  }
}
