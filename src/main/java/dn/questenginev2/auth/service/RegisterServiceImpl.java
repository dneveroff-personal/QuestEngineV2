package dn.questenginev2.auth.service;

import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.auth.dto.RegisterRequest;
import dn.questenginev2.common.exceptions.UserAlreadyExistsException;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private final UserService userService;
    private final LoginService loginService;
    private final PasswordEncoder passwordEncoder;

    // ────── IMPLEMENTATIONS ───────────────────────────────────────────────────────────
    @Override
    public LoginResponse register(RegisterRequest requestDto) {
        validateUserForRegistration(requestDto.getUsername(), requestDto.getEmail());

        User user = buildUser(requestDto);
        user = userService.saveUser(user);

        return loginService.login(requestDto, user);
    }

    // ────── VALIDATIONS ───────────────────────────────────────────────────────────
    @Override
    public void validateUserForRegistration(String username, String email) {
        if (userService.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username already exists");
        }
        if (email != null && !email.isBlank() && userService.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already exists");
        }
    }

    // ────── BUILDERS ───────────────────────────────────────────────────────────
    private User buildUser(RegisterRequest requestDto) {
        String passwordHash = passwordEncoder.encode(requestDto.getPassword());
        User user = new User();

        user.setUsername(requestDto.getUsername());
        user.setPublicName(requestDto.getPublicName() != null && !requestDto.getPublicName().isBlank()
                ? requestDto.getPublicName()
                : requestDto.getUsername());
        user.setEmail(requestDto.getEmail());
        user.setPasswordHash(passwordHash);
        user.setRole(UserRole.PLAYER);

        return user;
    }
}
