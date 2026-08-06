package dn.questenginev2.user.controller;

import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.user.dto.ResetPasswordRequest;
import dn.questenginev2.user.dto.SetRoleRequest;
import dn.questenginev2.user.dto.UserFilterRequest;
import dn.questenginev2.user.dto.UserResponse;
import dn.questenginev2.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.USERS)
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

  private final UserService userService;

  @Operation(summary = "Set user role", description = "Set role for a user")
  @PutMapping(Routes.SET_ROLE)
  public ResponseEntity<UserResponse> setUserRole(
      @PathVariable Long userId, @Valid @RequestBody SetRoleRequest request, Authentication auth) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(userService.setUserRole(userId, request.role(), auth));
  }

  @Operation(summary = "Reset user password", description = "Reset password for a user")
  @PostMapping(Routes.RESET_PASSWORD)
  public ResponseEntity<Void> resetPassword(
      @PathVariable Long userId,
      @Valid @RequestBody ResetPasswordRequest request,
      Authentication auth) {
    userService.resetPassword(userId, request, auth);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @Operation(
      summary = "Search users",
      description = "Search users with dynamic filters (username, email, role, date range)")
  @GetMapping("/search")
  public ResponseEntity<List<UserResponse>> searchUsers(
      @Valid UserFilterRequest filter, Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK).body(userService.searchUsers(filter, pageable));
  }
}
