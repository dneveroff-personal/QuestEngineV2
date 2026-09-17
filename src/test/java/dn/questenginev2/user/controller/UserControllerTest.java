package dn.questenginev2.user.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dn.questenginev2.common.dto.PageResponse;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.user.dto.UserResponse;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
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
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("Forbidden Operation")));
  }

  @Test
  void searchUsers_returnsPageResponse_whenUsersExist() throws Exception {
    // Arrange
    UserResponse user1 =
        new UserResponse(1L, "User One", "user1@example.com", UserRole.PLAYER, Instant.now());
    UserResponse user2 =
        new UserResponse(2L, "User Two", "user2@example.com", UserRole.AUTHOR, Instant.now());
    PageResponse<UserResponse> pageResponse =
        new PageResponse<>(List.of(user1, user2), 0, 20, 2L, 1);

    when(userService.searchUsers(any(), any(PageRequest.class))).thenReturn(pageResponse);

    // Act & Assert
    mockMvc
        .perform(get("/api/users/search"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].publicName").value("User One"))
        .andExpect(jsonPath("$.content[1].id").value(2))
        .andExpect(jsonPath("$.content[1].publicName").value("User Two"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  void searchUsers_returnsEmptyPage_whenNoUsersMatch() throws Exception {
    // Arrange
    PageResponse<UserResponse> pageResponse =
        new PageResponse<>(Collections.emptyList(), 0, 20, 0L, 0);

    when(userService.searchUsers(any(), any(PageRequest.class))).thenReturn(pageResponse);

    // Act & Assert
    mockMvc
        .perform(get("/api/users/search"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.totalPages").value(0));
  }

  @Test
  void searchUsers_returnsPageResponse_withPaginationParams() throws Exception {
    // Arrange
    UserResponse user1 =
        new UserResponse(1L, "User One", "user1@example.com", UserRole.PLAYER, Instant.now());
    PageResponse<UserResponse> pageResponse = new PageResponse<>(List.of(user1), 1, 10, 15L, 2);

    when(userService.searchUsers(any(), any(PageRequest.class))).thenReturn(pageResponse);

    // Act & Assert
    mockMvc
        .perform(get("/api/users/search").param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(15))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void searchUsers_returnsPageResponse_withFilterParams() throws Exception {
    // Arrange
    UserResponse user1 =
        new UserResponse(1L, "User One", "user1@example.com", UserRole.PLAYER, Instant.now());
    PageResponse<UserResponse> pageResponse = new PageResponse<>(List.of(user1), 0, 20, 1L, 1);

    when(userService.searchUsers(any(), any(PageRequest.class))).thenReturn(pageResponse);

    // Act & Assert
    mockMvc
        .perform(get("/api/users/search").param("username", "User"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].publicName").value("User One"));
  }
}
