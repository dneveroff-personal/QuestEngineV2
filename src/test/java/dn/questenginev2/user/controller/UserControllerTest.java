package dn.questenginev2.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dn.questenginev2.common.dto.PageResponse;
import dn.questenginev2.user.dto.UserResponse;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private UserService userService;

  private UserResponse userResponse;

  @BeforeEach
  void setUp() {
    userResponse =
        new UserResponse(1L, "testuser", "Test User", "test@example.com", UserRole.PLAYER, Instant.now());
  }

  @Test
  void setUserRole_returnsOk_whenValidRequest() throws Exception {
    when(userService.setUserRole(any(), any(), any())).thenReturn(userResponse);

    mockMvc
        .perform(
            put("/api/users/1/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"AUTHOR\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.publicName").value("Test User"));
  }

  @Test
  void resetPassword_returnsOk_whenValidRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/users/1/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"newPassword123\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void searchUsers_returnsPageResponse_whenUsersExist() throws Exception {
    UserResponse user1 =
        new UserResponse(1L, "user1", "User One", "user1@example.com", UserRole.PLAYER, Instant.now());
    UserResponse user2 =
        new UserResponse(2L, "user2", "User Two", "user2@example.com", UserRole.AUTHOR, Instant.now());
    PageResponse<UserResponse> pageResponse =
        new PageResponse<>(List.of(user1, user2), 0, 20, 2L, 1);

    when(userService.searchUsers(any(), any(PageRequest.class), any())).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/users/search"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void searchUsers_returnsEmptyPage_whenNoUsersMatch() throws Exception {
    PageResponse<UserResponse> pageResponse =
        new PageResponse<>(Collections.emptyList(), 0, 20, 0L, 0);

    when(userService.searchUsers(any(), any(PageRequest.class), any())).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/users/search"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void searchUsers_returnsPageResponse_withPaginationParams() throws Exception {
    UserResponse user1 =
        new UserResponse(1L, "user1", "User One", "user1@example.com", UserRole.PLAYER, Instant.now());
    PageResponse<UserResponse> pageResponse = new PageResponse<>(List.of(user1), 1, 10, 15L, 2);

    when(userService.searchUsers(any(), any(PageRequest.class), any())).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/users/search").param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(10));
  }

  @Test
  void searchUsers_returnsPageResponse_withFilterParams() throws Exception {
    UserResponse user1 =
        new UserResponse(1L, "user1", "User One", "user1@example.com", UserRole.PLAYER, Instant.now());
    PageResponse<UserResponse> pageResponse = new PageResponse<>(List.of(user1), 0, 20, 1L, 1);

    when(userService.searchUsers(any(), any(PageRequest.class), any())).thenReturn(pageResponse);

    mockMvc
        .perform(get("/api/users/search").param("username", "User"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1));
  }
}
