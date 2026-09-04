package dn.questenginev2.hint.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.hint.dto.HintProgressResponse;
import dn.questenginev2.hint.entity.HintType;
import dn.questenginev2.hint.service.HintProgressService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HintProgressController.class)
@Import(dn.questenginev2.config.test.TestSecurityConfig.class)
class HintProgressControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private HintProgressService hintProgressService;

  @Test
  void getShownHints_returnsShownHints_whenTeamHasActiveLevel() throws Exception {
    HintProgressResponse hint =
        HintProgressResponse.builder()
            .hintId(1L)
            .orderIndex(1)
            .content("Look under the bridge")
            .type(HintType.REGULAR)
            .shownAt(Instant.now())
            .build();
    when(hintProgressService.getShownHints(eq(1L), eq(2L), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(hint));

    mockMvc
        .perform(get("/api/quests/progress/1/2/hints"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].content").value("Look under the bridge"))
        .andExpect(jsonPath("$[0].type").value("REGULAR"));
  }

  @Test
  void getShownHints_returnsEmptyList_whenNoHintsShown() throws Exception {
    when(hintProgressService.getShownHints(eq(1L), eq(2L), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());

    mockMvc
        .perform(get("/api/quests/progress/1/2/hints"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void getShownHints_returnsConflict_whenUserNotTeamMember() throws Exception {
    when(hintProgressService.getShownHints(eq(1L), eq(2L), org.mockito.ArgumentMatchers.any()))
        .thenThrow(
            new ForbiddenOperationException("Видеть подсказки может только участник этой команды"));

    mockMvc
        .perform(get("/api/quests/progress/1/2/hints"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
