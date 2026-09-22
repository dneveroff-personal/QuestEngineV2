package dn.questenginev2.team.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dn.questenginev2.common.dto.PageResponse;
import dn.questenginev2.team.dto.TeamMemberDto;
import dn.questenginev2.team.dto.TeamResponse;
import dn.questenginev2.team.entity.TeamRole;
import dn.questenginev2.team.service.TeamService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TeamService teamService;

  private TeamResponse teamResponse;
  private TeamMemberDto memberDto;

  @BeforeEach
  void setUp() {
    teamResponse =
        new TeamResponse(
            1L, "Test Team", "testuser", "Test User", Instant.now(), Collections.emptyList());
    memberDto =
        new TeamMemberDto(1L, 1L, "member", "Member Display", TeamRole.MEMBER, Instant.now());
  }

  @Test
  void createTeam_returnsTeam_whenValidRequest() throws Exception {
    when(teamService.createTeam(any(), any())).thenReturn(teamResponse);

    mockMvc
        .perform(
            post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Team\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Test Team"))
        .andExpect(jsonPath("$.captainUsername").value("testuser"))
        .andExpect(jsonPath("$.captainDisplayName").value("Test User"));
  }

  @Test
  void getMyTeam_returnsTeam() throws Exception {
    when(teamService.getMyTeam(any())).thenReturn(teamResponse);

    mockMvc
        .perform(get("/api/teams/my"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.captainUsername").value("testuser"))
        .andExpect(jsonPath("$.captainDisplayName").value("Test User"));
  }

  @Test
  void getTeamMembers_returnsMembers_whenTeamExists() throws Exception {
    when(teamService.getTeamMembers(eq(1L))).thenReturn(Collections.singletonList(memberDto));

    mockMvc
        .perform(get("/api/teams/1/members"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].username").value("member"))
        .andExpect(jsonPath("$[0].displayName").value("Member Display"));
  }

  @Test
  void searchTeams_returnsPage() throws Exception {
    TeamResponse team1 =
        new TeamResponse(
            1L, "Team Alpha", "captain1", "Captain One", Instant.now(), Collections.emptyList());
    PageResponse<TeamResponse> pageResponse = new PageResponse<>(List.of(team1), 0, 20, 1L, 1);
    when(teamService.searchTeams(any(), any(PageRequest.class))).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/teams/search").param("name", "Alpha"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("Team Alpha"));
  }
}
