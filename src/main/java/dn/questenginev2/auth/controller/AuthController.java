package dn.questenginev2.auth.controller;

import dn.questenginev2.auth.dto.LoginRequest;
import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.auth.dto.LogoutRequest;
import dn.questenginev2.auth.dto.RefreshRequest;
import dn.questenginev2.auth.dto.ResetAdminPasswordRequest;
import dn.questenginev2.auth.service.JwtService;
import dn.questenginev2.auth.service.LoginService;
import dn.questenginev2.auth.service.RefreshTokenService;
import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.API)
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

  private final LoginService loginService;
  private final UserService userService;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  @Value("${admin.reset.secret:change-me-in-production}")
  private String adminResetSecret;

  @Operation(
      summary = "User login",
      description = "Authenticate and return access + refresh tokens")
  @PostMapping(Routes.LOGIN)
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = loginService.login(request);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Refresh tokens",
      description = "Exchange refresh token for new access + refresh pair (rotation, ADR-0015)")
  @PostMapping(Routes.REFRESH)
  public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    RefreshTokenService.RotatedTokens rotated = refreshTokenService.rotate(request.refreshToken());
    User user = rotated.user();
    String access = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
    return ResponseEntity.ok(
        new LoginResponse(user.getPublicName(), access, rotated.newRefreshToken()));
  }

  @Operation(summary = "Logout", description = "Revoke the presented refresh token")
  @PostMapping(Routes.LOGOUT)
  public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
    refreshTokenService.revoke(request.refreshToken());
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Reset admin password",
      description = "Reset admin password using admin secret")
  @PostMapping(Routes.RESET_ADMIN_PASSWORD)
  public ResponseEntity<Void> resetAdminPassword(
      @RequestHeader("X-Admin-Secret") String secret,
      @Valid @RequestBody ResetAdminPasswordRequest request) {
    if (!MessageDigest.isEqual(
        adminResetSecret.getBytes(StandardCharsets.UTF_8),
        secret.getBytes(StandardCharsets.UTF_8))) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    userService.resetAdminPassword(request);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
