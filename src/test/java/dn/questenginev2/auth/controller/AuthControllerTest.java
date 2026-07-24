package dn.questenginev2.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.auth.dto.LoginRequest;
import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.auth.service.LoginService;
import dn.questenginev2.auth.service.RegisterService;
import dn.questenginev2.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import(dn.questenginev2.config.test.TestSecurityConfig.class)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RegisterService registerService;

  @MockitoBean private LoginService loginService;

  @MockitoBean private UserService userService;

  @BeforeEach
  void setUp() {
    when(loginService.login(any(LoginRequest.class)))
        .thenReturn(new LoginResponse("Test User", "test-jwt-token"));
  }

  @Test
  void login_returnsToken_whenCredentialsAreValid() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"password123\"}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.publicName").value("Test User"))
        .andExpect(jsonPath("$.token").value("test-jwt-token"));
  }

  @Test
  void resetAdminPassword_returnsOk_whenSecretIsValid() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/reset-admin-password")
                .header("X-Admin-Secret", "change-me-in-production")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"newAdminPassword123\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void resetAdminPassword_returnsForbidden_whenSecretIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/reset-admin-password")
                .header("X-Admin-Secret", "wrong-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"newAdminPassword123\"}"))
        .andExpect(status().isForbidden());
  }
}
