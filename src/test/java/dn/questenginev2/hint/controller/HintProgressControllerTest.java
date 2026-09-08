package dn.questenginev2.hint.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
  void getVisibleHints_returnsShownHints_whenTeamHasActiveLevel() throws Exception {
    HintProgressResponse hint =
        HintProgressResponse.builder()
            .hintId(1L)
            .orderIndex(1)
            .content("Look under the bridge")
            .type(HintType.REGULAR)
            .shownAt(Instant.now())
            .build();
    when(hintProgressService.getVisibleHints(eq(1L), eq(2L), any())).thenReturn(List.of(hint));

    mockMvc
        .perform(get("/api/quests/progress/1/2/hints"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].content").value("Look under the bridge"))
        .andExpect(jsonPath("$[0].type").value("REGULAR"));
  }

  @Test
  void getVisibleHints_returnsAvailableNotTakenHint_withoutContent() throws Exception {
    HintProgressResponse hint =
        HintProgressResponse.builder().hintId(1L).orderIndex(1).type(HintType.PENALTY).build();
    when(hintProgressService.getVisibleHints(eq(1L), eq(2L), any())).thenReturn(List.of(hint));

    mockMvc
        .perform(get("/api/quests/progress/1/2/hints"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].type").value("PENALTY"))
        .andExpect(jsonPath("$[0].content").doesNotExist())
        .andExpect(jsonPath("$[0].bonusPenaltySeconds").doesNotExist());
  }

  @Test
  void getVisibleHints_returnsEmptyList_whenNoHintsShown() throws Exception {
    when(hintProgressService.getVisibleHints(eq(1L), eq(2L), any())).thenReturn(List.of());

    mockMvc
        .perform(get("/api/quests/progress/1/2/hints"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void getVisibleHints_returnsConflict_whenUserNotTeamMember() throws Exception {
    when(hintProgressService.getVisibleHints(eq(1L), eq(2L), any()))
        .thenThrow(
            new ForbiddenOperationException("Видеть подсказки может только участник этой команды"));

    mockMvc
        .perform(get("/api/quests/progress/1/2/hints"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void takeHint_returnsRevealedHint_whenAvailable() throws Exception {
    HintProgressResponse revealed =
        HintProgressResponse.builder()
            .hintId(1L)
            .orderIndex(1)
            .type(HintType.PENALTY)
            .content("The password is on the door")
            .bonusPenaltySeconds(600)
            .shownAt(Instant.now())
            .build();
    when(hintProgressService.takeHint(eq(1L), eq(2L), eq(1L), any())).thenReturn(revealed);

    mockMvc
        .perform(post("/api/quests/progress/1/2/hints/1/take"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("The password is on the door"))
        .andExpect(jsonPath("$.bonusPenaltySeconds").value(600));
  }

  @Test
  void takeHint_returnsConflict_whenNotYetAvailable() throws Exception {
    when(hintProgressService.takeHint(eq(1L), eq(2L), eq(1L), any()))
        .thenThrow(new ForbiddenOperationException("Подсказка ещё не стала доступна"));

    mockMvc
        .perform(post("/api/quests/progress/1/2/hints/1/take"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
