package dn.questenginev2.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.team.repository.TeamJoinRequestRepository;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.team.repository.TeamRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private TeamRepository teamRepository;

  @Autowired private QuestAuthorRepository questAuthorRepository;

  @Autowired private TeamMemberRepository teamMemberRepository;

  @Autowired private TeamJoinRequestRepository teamJoinRequestRepository;

  @Autowired private LevelRepository levelRepository;

  @Autowired private HintRepository hintRepository;

  @Autowired private CodeRepository codeRepository;

  @Autowired private QuestRepository questRepository;

  private User testUser;
  private String jwtToken;

  @BeforeEach
  void setUp() throws Exception {
    // Clean up in correct order to avoid foreign key constraints
    teamJoinRequestRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamRepository.deleteAll();
    questAuthorRepository.deleteAll();
    codeRepository.deleteAll();
    hintRepository.deleteAll();
    levelRepository.deleteAll();
    questRepository.deleteAll();
    userRepository.deleteAll();

    // Register a user and get JWT token
    String response =
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    jwtToken = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

    // Get the saved user
    testUser = userRepository.findByUsername("testuser").orElseThrow();
  }

  @Test
  void setUserRole_returnsConflict_whenUserIsNotAdmin() throws Exception {
    mockMvc
        .perform(
            put("/api/users/" + testUser.getId() + "/role")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"AUTHOR\"}"))
        .andExpect(status().isConflict());
  }
}
