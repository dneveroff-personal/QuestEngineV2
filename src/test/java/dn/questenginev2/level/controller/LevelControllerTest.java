package dn.questenginev2.level.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.level.dto.CreateLevelRequest;
import dn.questenginev2.level.dto.LevelResponse;
import dn.questenginev2.level.service.LevelService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LevelController.class)
@Import(dn.questenginev2.config.test.TestSecurityConfig.class)
class LevelControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LevelService levelService;

  private LevelResponse levelResponse;

  @BeforeEach
  void setUp() {
    levelResponse =
        new LevelResponse(
            1L, 1L, "Level 1", 1, "Level content", null, 300, Instant.now(), Instant.now());
  }

  @Test
  void createLevel_returnsCreatedLevel_whenRequestIsValid() throws Exception {
    when(levelService.createLevel(eq(1L), any(CreateLevelRequest.class), any()))
        .thenReturn(levelResponse);

    mockMvc
        .perform(
            post("/api/quests/1/levels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Level 1\",\"content\":\"Level content\",\"timeoutSeconds\":300}"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.questId").value(1))
        .andExpect(jsonPath("$.title").value("Level 1"))
        .andExpect(jsonPath("$.orderIndex").value(1))
        .andExpect(jsonPath("$.content").value("Level content"))
        .andExpect(jsonPath("$.timeoutSeconds").value(300));
  }

  @Test
  void createLevel_returnsConflict_whenUserIsNotAuthorOrAdmin() throws Exception {
    when(levelService.createLevel(eq(1L), any(CreateLevelRequest.class), any()))
        .thenThrow(
            new ForbiddenOperationException("Создавать уровни могут только AUTHOR или ADMIN"));

    mockMvc
        .perform(
            post("/api/quests/1/levels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Level 1\",\"content\":\"Level content\",\"timeoutSeconds\":300}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("Forbidden Operation")));
  }
}
