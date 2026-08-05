package dn.questenginev2.team.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.code.repository.CodeRepository;
import dn.questenginev2.hint.repository.HintRepository;
import dn.questenginev2.level.repository.LevelRepository;
import dn.questenginev2.quest.repository.QuestAuthorRepository;
import dn.questenginev2.quest.repository.QuestRepository;
import dn.questenginev2.team.dto.CreateTeamRequest;
import dn.questenginev2.team.entity.Team;
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
class TeamControllerIT {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private TeamRepository teamRepository;

  @Autowired private TeamMemberRepository teamMemberRepository;

  @Autowired private TeamJoinRequestRepository teamJoinRequestRepository;

  @Autowired private QuestAuthorRepository questAuthorRepository;

  @Autowired private LevelRepository levelRepository;

  @Autowired private HintRepository hintRepository;

  @Autowired private CodeRepository codeRepository;

  @Autowired private QuestRepository questRepository;

  private User testUser;
  private String jwtToken;

  @BeforeEach
  void setUp() throws Exception {
    teamJoinRequestRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamRepository.deleteAll();
    questAuthorRepository.deleteAll();
    codeRepository.deleteAll();
    hintRepository.deleteAll();
    levelRepository.deleteAll();
    questRepository.deleteAll();
    userRepository.deleteAll();

    // Get JWT token by registering
    String response =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Extract token from response (simple JSON parsing)
    jwtToken = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

    // Get the saved user
    testUser = userRepository.findByUsername("testuser").orElseThrow();
  }

  @Test
  void createTeam_returnsCreatedTeam_whenNameIsUnique() throws Exception {
    CreateTeamRequest request = new CreateTeamRequest("Unique Team Name");

    mockMvc
        .perform(
            post("/api/teams")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Unique Team Name\"}"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value("Unique Team Name"))
        .andExpect(jsonPath("$.captainName").value("testuser"))
        .andExpect(jsonPath("$.createdAt").isString());
  }

  @Test
  void createTeam_returnsConflict_whenTeamNameAlreadyExists() throws Exception {
    // Create existing team
    Team existingTeam = Team.builder().name("Existing Team").captain(testUser).build();
    teamRepository.save(existingTeam);

    mockMvc
        .perform(
            post("/api/teams")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Existing Team\"}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error", is("Team Already Exists")));
  }
}
