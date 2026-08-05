package dn.questenginev2.user.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.user.dto.UserResponse;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
@Import(dn.questenginev2.config.test.TestSecurityConfig.class)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  private UserResponse userResponse;

  @BeforeEach
  void setUp() {
    userResponse =
        new UserResponse(1L, "Test User", "test@example.com", UserRole.PLAYER, Instant.now());
  }

  @Test
  void setUserRole_returnsUpdatedUser_whenRequestIsValid() throws Exception {
    when(userService.setUserRole(eq(1L), eq(UserRole.AUTHOR), any())).thenReturn(userResponse);

    mockMvc
        .perform(
            put("/api/users/1/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"AUTHOR\"}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.publicName").value("Test User"))
        .andExpect(jsonPath("$.role").value("PLAYER"));
  }

  @Test
  void resetPassword_returnsOk_whenRequestIsValid() throws Exception {
    mockMvc
        .perform(
            post("/api/users/1/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"newPassword123\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void setUserRole_returnsConflict_whenUserIsNotAdmin() throws Exception {
    when(userService.setUserRole(eq(1L), eq(UserRole.AUTHOR), any()))
        .thenThrow(
            new ForbiddenOperationException("Данная операция разрешена только Администратору"));

    mockMvc
        .perform(
            put("/api/users/1/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"AUTHOR\"}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("Forbidden Operation")));
  }
}
