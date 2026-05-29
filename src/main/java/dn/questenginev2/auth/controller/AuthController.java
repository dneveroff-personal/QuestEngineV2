package dn.questenginev2.auth.controller;

import dn.questenginev2.auth.dto.LoginRequest;
import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.auth.service.LoginService;
import dn.questenginev2.common.constants.Routes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.API)
public class AuthController {

    private final LoginService loginService;

    @PostMapping(Routes.LOGIN)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginService.login(request);
        return ResponseEntity.ok(response);
    }

}
