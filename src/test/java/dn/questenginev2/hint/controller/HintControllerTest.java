package dn.questenginev2.hint.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.hint.dto.CreateHintRequest;
import dn.questenginev2.hint.dto.HintResponse;
import dn.questenginev2.hint.service.HintService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HintController.class)
@Import(dn.questenginev2.config.test.TestSecurityConfig.class)
class HintControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private HintService hintService;

  private HintResponse hintResponse;
  private HintResponse hintResponse2;

  @BeforeEach
  void setUp() {
    hintResponse = new HintResponse(1L, 1L, 1, 30, "Hint content", Instant.now(), Instant.now());

    hintResponse2 = new HintResponse(2L, 1L, 2, 60, "Hint content 2", Instant.now(), Instant.now());
  }

  @Test
  void createHint_returnsCreatedHint_whenRequestIsValid() throws Exception {
    when(hintService.createHint(eq(1L), any(CreateHintRequest.class), any()))
        .thenReturn(hintResponse);

    mockMvc
        .perform(
            post("/api/quests/1/levels/1/hints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderIndex\":1,\"delaySeconds\":30,\"content\":\"Hint content\"}"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.levelId").value(1))
        .andExpect(jsonPath("$.orderIndex").value(1))
        .andExpect(jsonPath("$.delaySeconds").value(30))
        .andExpect(jsonPath("$.content").value("Hint content"));
  }

  @Test
  void createHint_returnsConflict_whenUserIsNotAuthorOrAdmin() throws Exception {
    when(hintService.createHint(eq(1L), any(CreateHintRequest.class), any()))
        .thenThrow(
            new ForbiddenOperationException("Создавать подсказки могут только AUTHOR или ADMIN"));

    mockMvc
        .perform(
            post("/api/quests/1/levels/1/hints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderIndex\":1,\"delaySeconds\":30,\"content\":\"Hint content\"}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error", is("Forbidden Operation")));
  }

  @Test
  void getHintsByLevel_returnsHints_whenLevelExists() throws Exception {
    when(hintService.getHintsByLevelId(1L)).thenReturn(List.of(hintResponse, hintResponse2));

    mockMvc
        .perform(get("/api/quests/1/levels/1/hints"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].orderIndex").value(1))
        .andExpect(jsonPath("$[1].id").value(2))
        .andExpect(jsonPath("$[1].orderIndex").value(2));
  }

  @Test
  void getHintById_returnsHint_whenHintExists() throws Exception {
    when(hintService.getHintById(1L)).thenReturn(hintResponse);

    mockMvc
        .perform(get("/api/hints/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.content").value("Hint content"));
  }
}
