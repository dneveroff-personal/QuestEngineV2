package dn.questenginev2.auth.service;

import dn.questenginev2.auth.dto.AuthRequestBase;
import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

  private final UserService userService;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  @Override
  @Transactional
  public LoginResponse login(AuthRequestBase request) {
    User user =
        userService
            .findByUsername(request.getUsername())
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found: " + request.getUsername()));

    validatePassword(request.getPassword(), user.getPasswordHash());

    return buildLoginResponse(user);
  }

  @Override
  @Transactional
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
    String accessToken =
        jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
    String refreshToken = refreshTokenService.issue(user);
    return new LoginResponse(user.getPublicName(), accessToken, refreshToken);
  }
}
