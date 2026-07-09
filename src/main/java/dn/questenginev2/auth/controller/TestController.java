package dn.questenginev2.auth.controller;

import dn.questenginev2.auth.dto.LoginRequest;
import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.auth.service.LoginService;
import dn.questenginev2.common.constants.Routes;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
@Tag(name = "Test", description = "Test endpoints")
public class TestController {

    private final UserService userService;

    @Operation(summary = "Secure test endpoint", description = "Test endpoint that requires authentication")
    @GetMapping("/secure")
    public String secure() {
        // Можно внедрить непосредственно в метод как параметр
        // public User getCurrentUser(Authentication authentication) {
        //    String username = authentication.getName();

        // Тест получения имени пользователя
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Object roles = auth.getAuthorities(); //Роли

        // Тест загрузки сущности пользователя
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));

        return "Yes, it's secure!";
    }

}
