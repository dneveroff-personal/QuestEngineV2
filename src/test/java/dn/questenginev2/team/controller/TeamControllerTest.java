package dn.questenginev2.team.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.common.exceptions.RequestNotFoundException;
import dn.questenginev2.common.exceptions.TeamAlreadyExistsException;
import dn.questenginev2.common.exceptions.UserAlreadyInTeamException;
import dn.questenginev2.common.exceptions.UserNotFoundException;
import dn.questenginev2.config.test.TestSecurityConfig;
import dn.questenginev2.team.dto.*;
import dn.questenginev2.team.entity.JoinRequestType;
import dn.questenginev2.team.entity.TeamRole;
import dn.questenginev2.team.service.TeamService;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TeamController.class)
@Import(TestSecurityConfig.class)
class TeamControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TeamService teamService;

  private TeamResponse teamResponse;
  private TeamJoinResponse joinResponse;
  private TeamMemberDto memberDto;

  @BeforeEach
  void setUp() {
    teamResponse =
        new TeamResponse(1L, "Test Team", "Test User", Instant.now(), Collections.emptyList());
    joinResponse =
        new TeamJoinResponse(1L, "requester", JoinRequestType.JOIN_REQUEST, Instant.now());
    memberDto = new TeamMemberDto(1L, 1L, "member", TeamRole.MEMBER, Instant.now());
  }

  @Test
  void createTeam_returnsCreatedTeam_whenRequestIsValid() throws Exception {
    // Arrange
    when(teamService.createTeam(any(CreateTeamRequest.class), any())).thenReturn(teamResponse);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Team\"}"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Test Team"))
        .andExpect(jsonPath("$.captainName").value("Test User"));
  }

  @Test
  void createJoinRequest_returnsTrue_whenRequestIsValid() throws Exception {
    // Arrange
    when(teamService.createJoinRequest(any(), eq(1L), eq(null))).thenReturn(true);

    // Act & Assert
    mockMvc
        .perform(post("/api/teams/1/request"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", is(true)));
  }

  @Test
  void createJoinRequest_returnsTrue_whenUsernameProvided() throws Exception {
    // Arrange
    when(teamService.createJoinRequest(any(), eq(1L), eq("inviteduser"))).thenReturn(true);

    // Act & Assert
    mockMvc
        .perform(post("/api/teams/1/request").param("username", "inviteduser"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", is(true)));
  }

  @Test
  void approveRequest_returnsTrue_whenRequestIsValid() throws Exception {
    // Arrange
    when(teamService.approveRequest(eq(1L), any())).thenReturn(true);

    // Act & Assert
    mockMvc
        .perform(post("/api/teams/requests/1/approve"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", is(true)));
  }

  @Test
  void rejectRequest_returnsTrue_whenRequestIsValid() throws Exception {
    // Arrange
    when(teamService.rejectRequest(eq(1L), any())).thenReturn(true);

    // Act & Assert
    mockMvc
        .perform(post("/api/teams/requests/1/reject"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", is(true)));
  }

  @Test
  void leaveTeam_returnsTrue_whenRequestIsValid() throws Exception {
    // Arrange
    when(teamService.leaveTeam(any())).thenReturn(true);

    // Act & Assert
    mockMvc
        .perform(delete("/api/teams/leave"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", is(true)));
  }

  @Test
  void transferCaptain_returnsTrue_whenRequestIsValid() throws Exception {
    // Arrange
    when(teamService.transferCaptain(eq(2L), any())).thenReturn(true);

    // Act & Assert
    mockMvc
        .perform(post("/api/teams/transfer-captain/2"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", is(true)));
  }

  @Test
  void getJoinRequests_returnsRequests_whenUserIsAuthenticated() throws Exception {
    // Arrange
    when(teamService.getJoinRequests(any())).thenReturn(Collections.singletonList(joinResponse));

    // Act & Assert
    mockMvc
        .perform(get("/api/teams/requests"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].requestId").value(1))
        .andExpect(jsonPath("$[0].userName").value("requester"))
        .andExpect(jsonPath("$[0].type").value("JOIN_REQUEST"));
  }

  @Test
  void getJoinRequests_returnsEmptyList_whenNoRequests() throws Exception {
    // Arrange
    when(teamService.getJoinRequests(any())).thenReturn(Collections.emptyList());

    // Act & Assert
    mockMvc
        .perform(get("/api/teams/requests"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void getMyTeam_returnsTeam_whenUserIsInTeam() throws Exception {
    // Arrange
    when(teamService.getMyTeam(any())).thenReturn(teamResponse);

    // Act & Assert
    mockMvc
        .perform(get("/api/teams/my"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Test Team"))
        .andExpect(jsonPath("$.captainName").value("Test User"));
  }

  @Test
  void getTeamMembers_returnsMembers_whenTeamExists() throws Exception {
    // Arrange
    when(teamService.getTeamMembers(eq(1L))).thenReturn(Collections.singletonList(memberDto));

    // Act & Assert
    mockMvc
        .perform(get("/api/teams/1/members"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].name").value("member"))
        .andExpect(jsonPath("$[0].role").value("MEMBER"));
  }

  @Test
  void getTeamMembers_returnsEmptyList_whenTeamHasNoMembers() throws Exception {
    // Arrange
    when(teamService.getTeamMembers(eq(1L))).thenReturn(Collections.emptyList());

    // Act & Assert
    mockMvc
        .perform(get("/api/teams/1/members"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void createTeam_returnsConflict_whenTeamAlreadyExists() throws Exception {
    // Arrange
    when(teamService.createTeam(any(CreateTeamRequest.class), any()))
        .thenThrow(new TeamAlreadyExistsException("Team with name Test Team already exists"));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Team\"}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("Team Already Exists")));
  }

  @Test
  void createJoinRequest_returnsConflict_whenUserAlreadyInTeam() throws Exception {
    // Arrange
    when(teamService.createJoinRequest(any(), eq(1L), eq(null)))
        .thenThrow(new UserAlreadyInTeamException("User already member of a team"));

    // Act & Assert
    mockMvc
        .perform(post("/api/teams/1/request"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("User Already In Team")));
  }

  @Test
  void createJoinRequest_returnsNotFound_whenUserNotFound() throws Exception {
    // Arrange
    when(teamService.createJoinRequest(any(), eq(1L), eq("nonexistent")))
        .thenThrow(new UserNotFoundException("Приглашаемый пользователь не найден: nonexistent"));

    // Act & Assert
    mockMvc
        .perform(post("/api/teams/1/request").param("username", "nonexistent"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("User Not Found")));
  }

  @Test
  void approveRequest_returnsNotFound_whenRequestNotFound() throws Exception {
    // Arrange
    when(teamService.approveRequest(eq(999L), any()))
        .thenThrow(new RequestNotFoundException("Request not found"));

    // Act & Assert
    mockMvc
        .perform(post("/api/teams/requests/999/approve"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("Request Not Found")));
  }
}
