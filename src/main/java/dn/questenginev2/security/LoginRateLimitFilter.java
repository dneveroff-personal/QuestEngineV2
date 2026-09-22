package dn.questenginev2.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiter for {@code POST /api/auth/login} — 5 attempts per minute per IP (ADR-0016).
 *
 * <p>In-memory storage is intentional for the current single-instance deployment. CodeSubmission is
 * explicitly excluded from rate limiting (product decision).
 */
@Slf4j
public class LoginRateLimitFilter extends OncePerRequestFilter {

  private static final String LOGIN_PATH = "/api/auth/login";
  private static final int CAPACITY = 5;
  private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  /**
   * Очистить все бакеты (используется в тестах для сброса лимита между тестами).
   */
  public void clear() {
    buckets.clear();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !("POST".equalsIgnoreCase(request.getMethod())
        && LOGIN_PATH.equals(request.getRequestURI()));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String clientIp = resolveClientIp(request);
    Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
      return;
    }

    log.warn("Rate limit exceeded for login attempts from IP: {}", clientIp);
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response
        .getWriter()
        .write(
            """
{
  "type": "https://api.questenginev2.dn/problems/too-many-requests",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Too many login attempts. Limit is 5 requests per minute per IP. Please try again later."
}
""");
  }

  private Bucket createBucket(String key) {
    Bandwidth limit =
        Bandwidth.builder().capacity(CAPACITY).refillGreedy(CAPACITY, REFILL_PERIOD).build();
    return Bucket.builder().addLimit(limit).build();
  }

  /**
   * Prefer {@code X-Forwarded-For} (first hop) when present (reverse-proxy), otherwise fall back to
   * {@code remoteAddr}.
   */
  private String resolveClientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      // X-Forwarded-For may contain a chain: client, proxy1, proxy2
      return xff.split(",")[0].trim();
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    return request.getRemoteAddr();
  }
}
