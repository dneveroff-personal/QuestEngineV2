package dn.questenginev2.auth.controller;

import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.auth.dto.RegisterRequest;
import dn.questenginev2.auth.service.RegisterService;
import dn.questenginev2.common.constants.Routes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(Routes.API)
@Tag(name = "Registration", description = "User registration endpoints")
public class RegisterController {

  private final RegisterService registerService;

  @Operation(summary = "Register new user", description = "Create a new user account")
  @PostMapping(Routes.REGISTER)
  public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
    LoginResponse created = registerService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
