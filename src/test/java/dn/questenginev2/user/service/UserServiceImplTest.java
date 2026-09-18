package dn.questenginev2.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import dn.questenginev2.common.dto.PageResponse;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.UserNotFoundException;
import dn.questenginev2.user.dto.UserFilterRequest;
import dn.questenginev2.user.dto.UserResponse;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.repository.UserRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private Authentication authentication;
  @InjectMocks private UserServiceImpl userService;

  private User testUser;
  private User adminUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setPublicName("Test User");
    testUser.setEmail("test@example.com");
    testUser.setPasswordHash("hashedPassword");
    testUser.setRole(UserRole.PLAYER);
    testUser.setCreatedAt(Instant.now());

    adminUser = new User();
    adminUser.setId(2L);
    adminUser.setUsername("admin");
    adminUser.setPublicName("Admin User");
    adminUser.setEmail("admin@example.com");
    adminUser.setPasswordHash("hashedAdminPassword");
    adminUser.setRole(UserRole.ADMIN);
    adminUser.setCreatedAt(Instant.now());
  }

  @Test
  void searchUsers_returnsPageResponse_whenUsersExist() {
    when(authentication.getName()).thenReturn("admin");
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

    UserFilterRequest filter = new UserFilterRequest(null, null, null, null, null);
    PageRequest pageable = PageRequest.of(0, 20);

    User user1 = new User();
    user1.setId(1L);
    user1.setUsername("user1");
    user1.setPublicName("User One");
    user1.setEmail("user1@example.com");
    user1.setRole(UserRole.PLAYER);
    user1.setCreatedAt(Instant.now());

    User user2 = new User();
    user2.setId(2L);
    user2.setUsername("user2");
    user2.setPublicName("User Two");
    user2.setEmail("user2@example.com");
    user2.setRole(UserRole.AUTHOR);
    user2.setCreatedAt(Instant.now());

    Page<User> userPage = new PageImpl<>(List.of(user1, user2), pageable, 2);
    when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);

    PageResponse<UserResponse> result = userService.searchUsers(filter, pageable, authentication);

    assertThat(result).isNotNull();
    assertThat(result.content()).hasSize(2);
    assertThat(result.content().get(0).username()).isEqualTo("user1");
    assertThat(result.content().get(0).email()).isEqualTo("user1@example.com");
  }

  @Test
  void searchUsers_returnsEmptyPage_whenNoUsersMatch() {
    when(authentication.getName()).thenReturn("admin");
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

    UserFilterRequest filter = new UserFilterRequest("nonexistent", null, null, null, null);
    PageRequest pageable = PageRequest.of(0, 20);
    Page<User> userPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
    when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);

    PageResponse<UserResponse> result = userService.searchUsers(filter, pageable, authentication);

    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isEqualTo(0);
  }

  @Test
  void searchUsers_redactsSensitiveFields_whenCallerIsNotAdmin() {
    when(authentication.getName()).thenReturn("testuser");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    UserFilterRequest filter = new UserFilterRequest(null, null, null, null, null);
    PageRequest pageable = PageRequest.of(0, 20);

    User other = new User();
    other.setId(3L);
    other.setUsername("other");
    other.setPublicName("Other User");
    other.setEmail("other@example.com");
    other.setRole(UserRole.AUTHOR);
    other.setCreatedAt(Instant.now());

    Page<User> userPage = new PageImpl<>(List.of(other), pageable, 1);
    when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);

    PageResponse<UserResponse> result = userService.searchUsers(filter, pageable, authentication);

    assertThat(result.content()).hasSize(1);
    UserResponse row = result.content().get(0);
    assertThat(row.id()).isEqualTo(3L);
    assertThat(row.username()).isEqualTo("other");
    assertThat(row.publicName()).isEqualTo("Other User");
    assertThat(row.email()).isNull();
    assertThat(row.role()).isNull();
    assertThat(row.createdAt()).isNull();
  }

  @Test
  void setUserRole_setsRole_whenUserIsAdmin() {
    when(authentication.getName()).thenReturn("admin");
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    UserResponse response = userService.setUserRole(1L, UserRole.AUTHOR, authentication);

    assertThat(response).isNotNull();
    assertThat(response.role()).isEqualTo(UserRole.AUTHOR);
    verify(userRepository).save(testUser);
  }

  @Test
  void setUserRole_throwsForbiddenOperationException_whenUserIsNotAdmin() {
    when(authentication.getName()).thenReturn("testuser");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    assertThatThrownBy(() -> userService.setUserRole(1L, UserRole.AUTHOR, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("Администратору");

    verify(userRepository, never()).findById(any());
    verify(userRepository, never()).save(any());
  }

  @Test
  void getCurrentUser_returnsUser_whenFound() {
    when(authentication.getName()).thenReturn("testuser");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    User result = userService.getCurrentUser(authentication);

    assertThat(result.getUsername()).isEqualTo("testuser");
  }

  @Test
  void getCurrentUser_throwsUserNotFoundException_whenMissing() {
    when(authentication.getName()).thenReturn("missing");
    when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getCurrentUser(authentication))
        .isInstanceOf(UserNotFoundException.class);
  }
}
