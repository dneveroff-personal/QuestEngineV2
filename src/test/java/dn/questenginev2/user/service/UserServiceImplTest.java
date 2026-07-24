package dn.questenginev2.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.UserNotFoundException;
import dn.questenginev2.user.dto.ResetPasswordRequest;
import dn.questenginev2.user.dto.UserResponse;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
  void saveUser_savesAndReturnsUser() {
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    User saved = userService.saveUser(testUser);

    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isEqualTo(1L);
    verify(userRepository).save(testUser);
  }

  @Test
  void findByUsername_returnsUser_whenUserExists() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    Optional<User> result = userService.findByUsername("testuser");

    assertThat(result).isPresent();
    assertThat(result.get().getUsername()).isEqualTo("testuser");
    verify(userRepository).findByUsername("testuser");
  }

  @Test
  void findByUsername_returnsEmpty_whenUserDoesNotExist() {
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    Optional<User> result = userService.findByUsername("nonexistent");

    assertThat(result).isEmpty();
    verify(userRepository).findByUsername("nonexistent");
  }

  @Test
  void existsByUsername_returnsTrue_whenUserExists() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    boolean exists = userService.existsByUsername("testuser");

    assertThat(exists).isTrue();
    verify(userRepository).findByUsername("testuser");
  }

  @Test
  void existsByUsername_returnsFalse_whenUserDoesNotExist() {
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    boolean exists = userService.existsByUsername("nonexistent");

    assertThat(exists).isFalse();
    verify(userRepository).findByUsername("nonexistent");
  }

  @Test
  void existsByEmail_returnsTrue_whenEmailExists() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    boolean exists = userService.existsByEmail("test@example.com");

    assertThat(exists).isTrue();
    verify(userRepository).findByEmail("test@example.com");
  }

  @Test
  void existsByEmail_returnsFalse_whenEmailDoesNotExist() {
    when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

    boolean exists = userService.existsByEmail("nonexistent@example.com");

    assertThat(exists).isFalse();
    verify(userRepository).findByEmail("nonexistent@example.com");
  }

  @Test
  void getCurrentUser_returnsUser_whenUserExists() {
    when(authentication.getName()).thenReturn("testuser");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    User result = userService.getCurrentUser(authentication);

    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("testuser");
    verify(authentication).getName();
    verify(userRepository).findByUsername("testuser");
  }

  @Test
  void getCurrentUser_throwsUserNotFoundException_whenUserDoesNotExist() {
    when(authentication.getName()).thenReturn("nonexistent");
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getCurrentUser(authentication))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("nonexistent");

    verify(authentication).getName();
    verify(userRepository).findByUsername("nonexistent");
  }

  @Test
  void getUser_returnsUser_whenUserExists() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    User result = userService.getUser(1L);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    verify(userRepository).findById(1L);
  }

  @Test
  void getUser_throwsUserNotFoundException_whenUserDoesNotExist() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUser(999L))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("999");

    verify(userRepository).findById(999L);
  }

  @Test
  void setUserRole_setsRole_whenUserIsAdmin() {
    when(authentication.getName()).thenReturn("admin");
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    UserResponse response = userService.setUserRole(1L, UserRole.AUTHOR, authentication);

    assertThat(response).isNotNull();
    assertThat(response.getRole()).isEqualTo(UserRole.AUTHOR);
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
  void resetPassword_resetsPassword_whenUserIsAdmin() {
    when(authentication.getName()).thenReturn("admin");
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setNewPassword("newPassword");

    userService.resetPassword(1L, request, authentication);

    verify(passwordEncoder).encode("newPassword");
    verify(userRepository).save(testUser);
    assertThat(testUser.getPasswordHash()).isEqualTo("encodedNewPassword");
  }

  @Test
  void resetPassword_throwsForbiddenOperationException_whenUserIsNotAdmin() {
    when(authentication.getName()).thenReturn("testuser");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setNewPassword("newPassword");

    assertThatThrownBy(() -> userService.resetPassword(1L, request, authentication))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasMessageContaining("Администратору");

    verify(userRepository, never()).findById(any());
    verify(userRepository, never()).save(any());
  }

  @Test
  void resetAdminPassword_resetsAdminPassword() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
    when(passwordEncoder.encode("newAdminPassword")).thenReturn("encodedNewAdminPassword");
    when(userRepository.save(any(User.class))).thenReturn(adminUser);

    dn.questenginev2.auth.dto.ResetAdminPasswordRequest request =
        new dn.questenginev2.auth.dto.ResetAdminPasswordRequest();
    request.setNewPassword("newAdminPassword");

    userService.resetAdminPassword(request);

    verify(passwordEncoder).encode("newAdminPassword");
    verify(userRepository).save(adminUser);
    assertThat(adminUser.getPasswordHash()).isEqualTo("encodedNewAdminPassword");
  }
}
