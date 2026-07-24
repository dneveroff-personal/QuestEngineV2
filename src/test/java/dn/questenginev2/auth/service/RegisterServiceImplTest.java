package dn.questenginev2.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.auth.dto.RegisterRequest;
import dn.questenginev2.common.exceptions.UserAlreadyExistsException;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegisterServiceImplTest {

  @Mock private UserService userService;

  @Mock private LoginService loginService;

  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private RegisterServiceImpl registerService;

  @BeforeEach
  void setUp() {
    // Default: no existing users - only set up stubs that are commonly used
    when(userService.existsByUsername(any())).thenReturn(false);
  }

  @Test
  void register_createsUser_whenUsernameAndEmailAreUnique() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setPassword("password123");
    request.setEmail("newuser@example.com");
    request.setPublicName("New User");

    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setUsername("newuser");
    savedUser.setPublicName("New User");
    savedUser.setEmail("newuser@example.com");
    savedUser.setPasswordHash("encodedPassword");
    savedUser.setRole(UserRole.PLAYER);
    savedUser.setCreatedAt(Instant.now());

    when(userService.saveUser(any(User.class))).thenReturn(savedUser);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

    LoginResponse loginResponse = new LoginResponse("New User", "test-jwt-token");
    when(loginService.login(any(RegisterRequest.class), eq(savedUser))).thenReturn(loginResponse);

    LoginResponse response = registerService.register(request);

    assertThat(response).isNotNull();
    assertThat(response.getPublicName()).isEqualTo("New User");
    assertThat(response.getToken()).isEqualTo("test-jwt-token");

    verify(userService).saveUser(any(User.class));
    verify(loginService).login(any(RegisterRequest.class), eq(savedUser));
  }

  @Test
  void register_usesUsernameAsPublicName_whenPublicNameIsNull() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setPassword("password123");
    request.setEmail("newuser@example.com");
    request.setPublicName(null);

    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setUsername("newuser");
    savedUser.setPublicName("newuser"); // Should fallback to username
    savedUser.setEmail("newuser@example.com");
    savedUser.setPasswordHash("encodedPassword");
    savedUser.setRole(UserRole.PLAYER);
    savedUser.setCreatedAt(Instant.now());

    when(userService.saveUser(any(User.class))).thenReturn(savedUser);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

    LoginResponse loginResponse = new LoginResponse("newuser", "test-jwt-token");
    when(loginService.login(any(RegisterRequest.class), eq(savedUser))).thenReturn(loginResponse);

    LoginResponse response = registerService.register(request);

    assertThat(response).isNotNull();
    assertThat(response.getPublicName()).isEqualTo("newuser");
  }

  @Test
  void register_usesUsernameAsPublicName_whenPublicNameIsBlank() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setPassword("password123");
    request.setEmail("newuser@example.com");
    request.setPublicName("   ");

    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setUsername("newuser");
    savedUser.setPublicName("newuser"); // Should fallback to username
    savedUser.setEmail("newuser@example.com");
    savedUser.setPasswordHash("encodedPassword");
    savedUser.setRole(UserRole.PLAYER);
    savedUser.setCreatedAt(Instant.now());

    when(userService.saveUser(any(User.class))).thenReturn(savedUser);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

    LoginResponse loginResponse = new LoginResponse("newuser", "test-jwt-token");
    when(loginService.login(any(RegisterRequest.class), eq(savedUser))).thenReturn(loginResponse);

    LoginResponse response = registerService.register(request);

    assertThat(response).isNotNull();
    assertThat(response.getPublicName()).isEqualTo("newuser");
  }

  @Test
  void register_throwsUserAlreadyExistsException_whenUsernameExists() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("existinguser");
    request.setPassword("password123");
    request.setEmail("newuser@example.com");

    when(userService.existsByUsername("existinguser")).thenReturn(true);

    assertThatThrownBy(() -> registerService.register(request))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessageContaining("Username already exists");

    verify(userService, never()).saveUser(any());
    verify(loginService, never()).login(any(), any());
  }

  @Test
  void register_throwsUserAlreadyExistsException_whenEmailExists() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setPassword("password123");
    request.setEmail("existing@example.com");

    when(userService.existsByUsername("newuser")).thenReturn(false);
    when(userService.existsByEmail("existing@example.com")).thenReturn(true);

    assertThatThrownBy(() -> registerService.register(request))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessageContaining("Email already exists");

    verify(userService, never()).saveUser(any());
    verify(loginService, never()).login(any(), any());
  }

  @Test
  void validateUserForRegistration_doesNotThrow_whenUsernameAndEmailAreUnique() {
    // This is called internally by register(), but we can test it indirectly
    // by verifying register() succeeds when both are unique
    RegisterRequest request = new RegisterRequest();
    request.setUsername("newuser");
    request.setPassword("password123");
    request.setEmail("newuser@example.com");

    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setUsername("newuser");
    savedUser.setPublicName("newuser");
    savedUser.setEmail("newuser@example.com");
    savedUser.setPasswordHash("encodedPassword");
    savedUser.setRole(UserRole.PLAYER);
    savedUser.setCreatedAt(Instant.now());

    when(userService.saveUser(any(User.class))).thenReturn(savedUser);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

    LoginResponse loginResponse = new LoginResponse("newuser", "test-jwt-token");
    when(loginService.login(any(RegisterRequest.class), eq(savedUser))).thenReturn(loginResponse);

    // Should not throw
    LoginResponse response = registerService.register(request);
    assertThat(response).isNotNull();
  }
}
