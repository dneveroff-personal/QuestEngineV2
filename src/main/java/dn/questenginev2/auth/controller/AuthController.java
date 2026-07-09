package dn.questenginev2.auth.controller;

import dn.questenginev2.auth.dto.LoginRequest;
import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.auth.dto.ResetAdminPasswordRequest;
import dn.questenginev2.auth.service.LoginService;
import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.API)
public class AuthController {

    private final LoginService loginService;
    private final UserService userService;

    @Value("${admin.reset.secret}")
    private String adminResetSecret;

    @PostMapping(Routes.LOGIN)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(Routes.RESET_ADMIN_PASSWORD)
    public ResponseEntity<Void> resetAdminPassword(
            @RequestHeader("X-Admin-Secret") String secret,
            @Valid @RequestBody ResetAdminPasswordRequest request
    ) {
        if (!adminResetSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.resetAdminPassword(request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
