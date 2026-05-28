package dn.questenginev2.auth.controller;

import dn.questenginev2.auth.dto.RegisterRequest;
import dn.questenginev2.auth.dto.RegisterResponse;
import dn.questenginev2.auth.service.RegisterService;
import dn.questenginev2.common.constants.Routes;
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
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping(Routes.REGISTER)
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            RegisterResponse created = registerService.register(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(created);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }
    }

}
