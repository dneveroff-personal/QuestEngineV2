package dn.questenginev2.auth.controller;

import dn.questenginev2.auth.dto.LoginRequest;
import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.auth.dto.ResetAdminPasswordRequest;
import dn.questenginev2.auth.service.LoginService;
import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Arrays;
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

  @Value("${admin.reset.secret:change-me-in-production}")
  private String adminResetSecret;

  @Operation(summary = "User login", description = "Authenticate user and return JWT token")
  @PostMapping(Routes.LOGIN)
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = loginService.login(request);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Reset admin password",
      description = "Reset admin password using admin secret")
  @PostMapping(Routes.RESET_ADMIN_PASSWORD)
  public ResponseEntity<Void> resetAdminPassword(
      @RequestHeader("X-Admin-Secret") String secret,
      @Valid @RequestBody ResetAdminPasswordRequest request) {
    if (!adminResetSecret.equals(secret)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    int[] nums = new int[] {2, 3, 1, 2, 4, 3};
    Arrays.sort(nums);

    userService.resetAdminPassword(request);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
