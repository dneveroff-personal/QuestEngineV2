package dn.questenginev2.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final PasswordEncoder passwordEncoder;

  @Value("${jwt.secret}")
  private String secret;

  /** Access token TTL in ms — ADR-0015: 15 minutes. */
  @Value("${jwt.access-expiration:900000}")
  private Long accessExpiration;

  /**
   * @deprecated use {@link #accessExpiration}; kept so existing {@code jwt.expiration} configs still
   *     apply if access-expiration is not set.
   */
  @Deprecated
  @Value("${jwt.expiration:900000}")
  private Long expiration;

  @Getter private SecretKey key;

  public JwtService(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
    if (accessExpiration == null || accessExpiration <= 0) {
      accessExpiration = expiration;
    }
  }

  public String generateAccessToken(String username, String role) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("role", role);

    long ttl = accessExpiration != null ? accessExpiration : expiration;
    return Jwts.builder()
        .claims(claims)
        .subject(username)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + ttl))
        .signWith(key)
        .compact();
  }

  /** @deprecated use {@link #generateAccessToken} */
  @Deprecated
  public String generateToken(String username, String role) {
    return generateAccessToken(username, role);
  }

  public boolean validatePassword(String rawPassword, String passwordHash) {
    return passwordEncoder.matches(rawPassword, passwordHash);
  }
}
