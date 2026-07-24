package dn.questenginev2.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dn.questenginev2.auth.dto.AuthRequestBase;
import dn.questenginev2.auth.dto.LoginResponse;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

  @Mock private UserService userService;

  @Mock private JwtService jwtService;

  @InjectMocks private LoginServiceImpl loginService;

  private User testUser;

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
  }

  @Test
  void login_returnsLoginResponse_whenCredentialsAreValid() {
    AuthRequestBase request = new AuthRequestBase() {};
    request.setUsername("testuser");
    request.setPassword("password123");

    when(userService.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
    when(jwtService.validatePassword("password123", "hashedPassword")).thenReturn(true);
    when(jwtService.generateToken("testuser", "PLAYER")).thenReturn("test-jwt-token");

    LoginResponse response = loginService.login(request);

    assertThat(response).isNotNull();
    assertThat(response.getPublicName()).isEqualTo("Test User");
    assertThat(response.getToken()).isEqualTo("test-jwt-token");
    verify(userService).findByUsername("testuser");
    verify(jwtService).validatePassword("password123", "hashedPassword");
    verify(jwtService).generateToken("testuser", "PLAYER");
  }

  @Test
  void login_throwsUsernameNotFoundException_whenUserDoesNotExist() {
    AuthRequestBase request = new AuthRequestBase() {};
    request.setUsername("nonexistent");
    request.setPassword("password123");

    when(userService.findByUsername("nonexistent")).thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> loginService.login(request))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("nonexistent");

    verify(userService).findByUsername("nonexistent");
    verify(jwtService, never()).validatePassword(any(), any());
  }

  @Test
  void login_throwsBadCredentialsException_whenPasswordIsInvalid() {
    AuthRequestBase request = new AuthRequestBase() {};
    request.setUsername("testuser");
    request.setPassword("wrongpassword");

    when(userService.findByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
    when(jwtService.validatePassword("wrongpassword", "hashedPassword")).thenReturn(false);

    assertThatThrownBy(() -> loginService.login(request))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Invalid password");

    verify(userService).findByUsername("testuser");
    verify(jwtService).validatePassword("wrongpassword", "hashedPassword");
    verify(jwtService, never()).generateToken(any(), any());
  }

  @Test
  void loginWithUser_returnsLoginResponse_whenCredentialsAreValid() {
    AuthRequestBase request = new AuthRequestBase() {};
    request.setUsername("testuser");
    request.setPassword("password123");

    when(jwtService.validatePassword("password123", "hashedPassword")).thenReturn(true);
    when(jwtService.generateToken("testuser", "PLAYER")).thenReturn("test-jwt-token");

    LoginResponse response = loginService.login(request, testUser);

    assertThat(response).isNotNull();
    assertThat(response.getPublicName()).isEqualTo("Test User");
    assertThat(response.getToken()).isEqualTo("test-jwt-token");
    verify(jwtService).validatePassword("password123", "hashedPassword");
    verify(jwtService).generateToken("testuser", "PLAYER");
  }

  @Test
  void loginWithUser_throwsBadCredentialsException_whenPasswordIsInvalid() {
    AuthRequestBase request = new AuthRequestBase() {};
    request.setUsername("testuser");
    request.setPassword("wrongpassword");

    when(jwtService.validatePassword("wrongpassword", "hashedPassword")).thenReturn(false);

    assertThatThrownBy(() -> loginService.login(request, testUser))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Invalid password");

    verify(jwtService).validatePassword("wrongpassword", "hashedPassword");
    verify(jwtService, never()).generateToken(any(), any());
  }
}
