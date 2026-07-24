package dn.questenginev2.quest.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.quest.dto.CreateQuestRequest;
import dn.questenginev2.quest.dto.QuestResponse;
import dn.questenginev2.quest.entity.QuestType;
import dn.questenginev2.quest.service.QuestService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = QuestController.class)
@Import(dn.questenginev2.config.test.TestSecurityConfig.class)
class QuestControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private QuestService questService;

  private QuestResponse questResponse;

  @BeforeEach
  void setUp() {
    questResponse =
        new QuestResponse(
            1L,
            "Test Quest",
            "Test Description",
            QuestType.TEAM,
            dn.questenginev2.quest.entity.QuestStatus.DRAFT,
            Instant.now(),
            Instant.now(),
            Instant.now());
  }

  @Test
  void createQuest_returnsCreatedQuest_whenRequestIsValid() throws Exception {
    when(questService.createQuest(any(CreateQuestRequest.class), any())).thenReturn(questResponse);

    mockMvc
        .perform(
            post("/api/quests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Test Quest\",\"description\":\"Test"
                        + " Description\",\"type\":\"TEAM\"}"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.title").value("Test Quest"))
        .andExpect(jsonPath("$.description").value("Test Description"))
        .andExpect(jsonPath("$.type").value("TEAM"))
        .andExpect(jsonPath("$.status").value("DRAFT"));
  }

  @Test
  void getQuestById_returnsQuest_whenQuestExists() throws Exception {
    when(questService.getQuestById(eq(1L))).thenReturn(questResponse);

    mockMvc
        .perform(get("/api/quests/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.title").value("Test Quest"));
  }

  @Test
  void updateQuest_returnsUpdatedQuest_whenRequestIsValid() throws Exception {
    QuestResponse updatedResponse =
        new QuestResponse(
            1L,
            "Updated Quest",
            "Updated Description",
            QuestType.SINGLE,
            dn.questenginev2.quest.entity.QuestStatus.PUBLISHED,
            Instant.now(),
            Instant.now(),
            Instant.now());
    when(questService.updateQuest(eq(1L), any(CreateQuestRequest.class), any()))
        .thenReturn(updatedResponse);

    mockMvc
        .perform(
            put("/api/quests/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"Updated Quest\",\"description\":\"Updated"
                        + " Description\",\"type\":\"SINGLE\"}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.title").value("Updated Quest"))
        .andExpect(jsonPath("$.type").value("SINGLE"))
        .andExpect(jsonPath("$.status").value("PUBLISHED"));
  }

  @Test
  void deleteQuest_returnsNoContent_whenQuestExists() throws Exception {
    mockMvc.perform(delete("/api/quests/1")).andExpect(status().isOk());
  }

  @Test
  void createQuest_returnsConflict_whenUserIsNotAuthorOrAdmin() throws Exception {
    when(questService.createQuest(any(CreateQuestRequest.class), any()))
        .thenThrow(
            new ForbiddenOperationException("Создавать квесты могут только AUTHOR или ADMIN"));

    mockMvc
        .perform(
            post("/api/quests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Quest\",\"description\":\"Test Description\"}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error", is("Forbidden Operation")));
  }
}
