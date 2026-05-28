package dn.questenginev2.auth.service;

import dn.questenginev2.auth.dto.RegisterRequest;
import dn.questenginev2.auth.dto.RegisterResponse;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterServiceImpl implements RegisterService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public RegisterServiceImpl(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterResponse register(RegisterRequest requestDto) {
        // validate
        userService.validateUserForRegistration(requestDto.getUsername(), requestDto.getEmail());

        // hash password
        String passwordHash = passwordEncoder.encode(requestDto.getPassword());

        // save user
        User user = new User();
        user.setUsername(requestDto.getUsername());
        user.setPublicName(requestDto.getPublicName() != null && !requestDto.getPublicName().isBlank()
                ? requestDto.getPublicName()
                : requestDto.getUsername());
        user.setEmail(requestDto.getEmail());
        user.setPasswordHash(passwordHash);
        user.setRole(UserRole.PLAYER);

        user = userService.saveUser(user);
        return toDTO(user);
    }

    private RegisterResponse toDTO(User user) {
        return new RegisterResponse(user.getPublicName(), false);
    }
}
