package dn.questenginev2.code.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.code.dto.CodeSubmissionResponse;
import dn.questenginev2.code.dto.SubmitCodeRequest;
import dn.questenginev2.code.entity.CodeSubmissionResult;
import dn.questenginev2.code.service.CodeSubmissionService;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CodeSubmissionController.class)
@Import(dn.questenginev2.config.test.TestSecurityConfig.class)
class CodeSubmissionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CodeSubmissionService codeSubmissionService;

  @Test
  void submitCode_returnsCorrectMain_whenCodeMatches() throws Exception {
    CodeSubmissionResponse response =
        CodeSubmissionResponse.builder()
            .result(CodeSubmissionResult.CORRECT_MAIN)
            .remainingMainCodes(1)
            .levelCompleted(false)
            .questFinished(false)
            .submittedAt(Instant.now())
            .build();
    when(codeSubmissionService.submitCode(eq(1L), eq(2L), any(SubmitCodeRequest.class), any()))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/quests/progress/1/2/codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"синий\"}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.result").value("CORRECT_MAIN"))
        .andExpect(jsonPath("$.remainingMainCodes").value(1))
        .andExpect(jsonPath("$.levelCompleted").value(false));
  }

  @Test
  void submitCode_returnsBadRequest_whenValueBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/quests/progress/1/2/codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitCode_returnsConflict_whenUserNotTeamMember() throws Exception {
    when(codeSubmissionService.submitCode(eq(1L), eq(2L), any(SubmitCodeRequest.class), any()))
        .thenThrow(
            new ForbiddenOperationException("Вводить коды может только участник этой команды"));

    mockMvc
        .perform(
            post("/api/quests/progress/1/2/codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"синий\"}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
