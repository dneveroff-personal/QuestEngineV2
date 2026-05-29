package dn.questenginev2.auth.service;

import dn.questenginev2.auth.dto.AuthRequestBase;
import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final UserService userService;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(AuthRequestBase request) {
        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getUsername()));

        validatePassword(request.getPassword(), user.getPasswordHash());

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse login(AuthRequestBase request, User user) {
        validatePassword(request.getPassword(), user.getPasswordHash());
        return buildLoginResponse(user);
    }

    private void validatePassword(String rawPassword, String passwordHash) {
        if (!jwtService.validatePassword(rawPassword, passwordHash)) {
            throw new BadCredentialsException("Invalid password");
        }
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(user.getPublicName(), token);
    }
}