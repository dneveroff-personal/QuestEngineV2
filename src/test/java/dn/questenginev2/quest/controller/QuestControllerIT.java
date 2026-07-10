package dn.questenginev2.quest.controller;

import dn.questenginev2.quest.dto.CreateQuestRequest;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QuestControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private QuestAuthorRepository questAuthorRepository;

    @Autowired
    private dn.questenginev2.level.repository.LevelRepository levelRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User authorUser;
    private String authorToken;

    @BeforeEach
    void setUp() throws Exception {
        levelRepository.deleteAll();
        questAuthorRepository.deleteAll();
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
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
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
        mockMvc.perform(post("/api/quests")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Quest\",\"description\":\"Test Description\",\"type\":\"TEAM\"}"))
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
        Quest quest = Quest.builder()
                .title("Test Quest")
                .description("Test Description")
                .type(QuestType.TEAM)
                .status(QuestStatus.DRAFT)
                .build();
        quest = questRepository.save(quest);

        // Create QuestAuthor record
        QuestAuthor questAuthor = QuestAuthor.builder()
                .quest(quest)
                .user(authorUser)
                .build();
        questAuthorRepository.save(questAuthor);

        mockMvc.perform(get("/api/quests/" + quest.getId())
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(quest.getId()))
                .andExpect(jsonPath("$.title").value("Test Quest"));
    }

    @Test
    void updateQuest_returnsUpdatedQuest_whenUserIsAuthorized() throws Exception {
        // Create a quest first
        Quest quest = Quest.builder()
                .title("Old Title")
                .description("Old Description")
                .type(QuestType.TEAM)
                .status(QuestStatus.DRAFT)
                .build();
        quest = questRepository.save(quest);

        // Create QuestAuthor record
        QuestAuthor questAuthor = QuestAuthor.builder()
                .quest(quest)
                .user(authorUser)
                .build();
        questAuthorRepository.save(questAuthor);

        mockMvc.perform(put("/api/quests/" + quest.getId())
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Title\",\"description\":\"New Description\",\"type\":\"SINGLE\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.type").value("SINGLE"));
    }

    @Test
    void deleteQuest_returnsNoContent_whenQuestExists() throws Exception {
        // Create a quest first
        Quest quest = Quest.builder()
                .title("Test Quest")
                .description("Test Description")
                .type(QuestType.TEAM)
                .status(QuestStatus.DRAFT)
                .build();
        quest = questRepository.save(quest);

        mockMvc.perform(delete("/api/quests/" + quest.getId())
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk());
    }
}
