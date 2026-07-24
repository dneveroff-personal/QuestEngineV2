package dn.questenginev2.code.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.code.dto.CodeResponse;
import dn.questenginev2.code.dto.CreateCodeRequest;
import dn.questenginev2.code.entity.CodeType;
import dn.questenginev2.code.service.CodeService;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
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

@WebMvcTest(controllers = CodeController.class)
@Import(dn.questenginev2.config.test.TestSecurityConfig.class)
class CodeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CodeService codeService;

  private CodeResponse codeResponse;

  @BeforeEach
  void setUp() {
    codeResponse = new CodeResponse(1L, 1L, "CODE123", CodeType.MAIN, 100, Instant.now());
  }

  @Test
  void createCode_returnsCreatedCode_whenRequestIsValid() throws Exception {
    when(codeService.createCode(eq(1L), any(CreateCodeRequest.class), any()))
        .thenReturn(codeResponse);

    mockMvc
        .perform(
            post("/api/quests/1/levels/1/codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"CODE123\",\"type\":\"MAIN\",\"points\":100}"))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.levelId").value(1))
        .andExpect(jsonPath("$.value").value("CODE123"))
        .andExpect(jsonPath("$.type").value("MAIN"))
        .andExpect(jsonPath("$.points").value(100));
  }

  @Test
  void createCode_returnsConflict_whenUserIsNotAuthorOrAdmin() throws Exception {
    when(codeService.createCode(eq(1L), any(CreateCodeRequest.class), any()))
        .thenThrow(new ForbiddenOperationException("Создавать коды могут только AUTHOR или ADMIN"));

    mockMvc
        .perform(
            post("/api/quests/1/levels/1/codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"CODE123\",\"type\":\"MAIN\",\"points\":100}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error", is("Forbidden Operation")));
  }

  @Test
  void getCodesByLevel_returnsCodes_whenLevelExists() throws Exception {
    CodeResponse codeResponse2 =
        new CodeResponse(2L, 1L, "CODE456", CodeType.BONUS, 50, Instant.now());

    when(codeService.getCodesByLevelId(1L)).thenReturn(List.of(codeResponse, codeResponse2));

    mockMvc
        .perform(get("/api/quests/1/levels/1/codes"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].value").value("CODE123"))
        .andExpect(jsonPath("$[1].id").value(2))
        .andExpect(jsonPath("$[1].value").value("CODE456"));
  }

  @Test
  void getCodeById_returnsCode_whenCodeExists() throws Exception {
    when(codeService.getCodeById(1L)).thenReturn(codeResponse);

    mockMvc
        .perform(get("/api/codes/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.value").value("CODE123"))
        .andExpect(jsonPath("$.type").value("MAIN"))
        .andExpect(jsonPath("$.points").value(100));
  }
}
