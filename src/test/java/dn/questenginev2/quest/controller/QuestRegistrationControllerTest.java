package dn.questenginev2.quest.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.quest.dto.QuestRegisterResponse;
import dn.questenginev2.quest.entity.RegistrationStatus;
import dn.questenginev2.quest.service.QuestRegistrationService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = QuestRegistrationController.class)
@Import(dn.questenginev2.config.test.TestSecurityConfig.class)
class QuestRegistrationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private QuestRegistrationService questRegistrationService;

  private QuestRegisterResponse registerResponse;

  @BeforeEach
  void setUp() {
    registerResponse = new QuestRegisterResponse(1L, 1L, "Test Team", RegistrationStatus.PENDING);
  }

  @Test
  void register_returnsAccepted_whenRequestIsValid() throws Exception {
    when(questRegistrationService.registerTeam(eq(1L), eq(1L), any())).thenReturn(registerResponse);

    mockMvc
        .perform(post("/api/quests/register/1/1"))
        .andExpect(status().isAccepted())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.questId").value(1))
        .andExpect(jsonPath("$.teamId").value(1))
        .andExpect(jsonPath("$.teamName").value("Test Team"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void getRegisteredTeams_returnsList_whenQuestExists() throws Exception {
    when(questRegistrationService.findAll(eq(1L)))
        .thenReturn(Collections.singletonList(registerResponse));

    mockMvc
        .perform(get("/api/quests/register/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].questId").value(1))
        .andExpect(jsonPath("$[0].teamId").value(1))
        .andExpect(jsonPath("$[0].teamName").value("Test Team"))
        .andExpect(jsonPath("$[0].status").value("PENDING"));
  }

  @Test
  void getRegisteredTeams_returnsEmptyList_whenNoRegistrations() throws Exception {
    when(questRegistrationService.findAll(eq(1L))).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/api/quests/register/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void unregisterTeam_returnsRegistration_whenRequestIsValid() throws Exception {
    when(questRegistrationService.unregisterTeam(eq(1L), any())).thenReturn(registerResponse);

    mockMvc
        .perform(delete("/api/quests/register/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.questId").value(1))
        .andExpect(jsonPath("$.teamId").value(1))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void approveTeam_returnsApprovedRegistration_whenRequestIsValid() throws Exception {
    QuestRegisterResponse approvedResponse =
        new QuestRegisterResponse(1L, 1L, "Test Team", RegistrationStatus.APPROVED);
    when(questRegistrationService.approveTeam(eq(1L), eq(1L), any())).thenReturn(approvedResponse);

    mockMvc
        .perform(put("/api/quests/register/1/approve/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.questId").value(1))
        .andExpect(jsonPath("$.teamId").value(1))
        .andExpect(jsonPath("$.status").value("APPROVED"));
  }

  @Test
  void rejectTeam_returnsRejectedRegistration_whenRequestIsValid() throws Exception {
    QuestRegisterResponse rejectedResponse =
        new QuestRegisterResponse(1L, 1L, "Test Team", RegistrationStatus.REJECTED);
    when(questRegistrationService.rejectTeam(eq(1L), eq(1L), any())).thenReturn(rejectedResponse);

    mockMvc
        .perform(put("/api/quests/register/1/teams/1/reject"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.questId").value(1))
        .andExpect(jsonPath("$.teamId").value(1))
        .andExpect(jsonPath("$.status").value("REJECTED"));
  }

  @Test
  void register_returnsConflict_whenForbiddenOperation() throws Exception {
    when(questRegistrationService.registerTeam(eq(1L), eq(1L), any()))
        .thenThrow(new ForbiddenOperationException("Подать заявку может только капитан команды"));

    mockMvc
        .perform(post("/api/quests/register/1/1"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("Forbidden Operation")));
  }

  @Test
  void approveTeam_returnsConflict_whenForbiddenOperation() throws Exception {
    when(questRegistrationService.approveTeam(eq(1L), eq(1L), any()))
        .thenThrow(
            new ForbiddenOperationException("Подтверждать заявки может только Автор квеста"));

    mockMvc
        .perform(put("/api/quests/register/1/approve/1"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("Forbidden Operation")));
  }
}
