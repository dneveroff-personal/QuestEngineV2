package dn.questenginev2.auth.service;

import dn.questenginev2.auth.dto.LogInRequest;
import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginServiceImpl(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponse login(LogInRequest request) {
        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());

        return new LoginResponse(user.getPublicName(), true, token);
    }
}