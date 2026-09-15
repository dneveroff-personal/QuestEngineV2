package dn.questenginev2.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitFilterTest {

  private LoginRateLimitFilter filter;

  @Mock private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    filter = new LoginRateLimitFilter();
  }

  @Test
  void shouldNotFilter_otherPaths() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
    assertThat(filter.shouldNotFilter(request)).isTrue();
  }

  @Test
  void shouldNotFilter_getLogin() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
    assertThat(filter.shouldNotFilter(request)).isTrue();
  }

  @Test
  void shouldFilter_postLogin() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    assertThat(filter.shouldNotFilter(request)).isFalse();
  }

  @Test
  void allowsUpToFiveRequestsFromSameIp() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setRemoteAddr("10.0.0.1");
    MockHttpServletResponse response = new MockHttpServletResponse();

    for (int i = 0; i < 5; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(5)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    assertThat(response.getStatus()).isEqualTo(200); // default of MockHttpServletResponse
  }

  @Test
  void blocksSixthRequestFromSameIp() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setRemoteAddr("10.0.0.2");

    for (int i = 0; i < 5; i++) {
      MockHttpServletResponse okResponse = new MockHttpServletResponse();
      filter.doFilterInternal(request, okResponse, filterChain);
    }

    MockHttpServletResponse blocked = new MockHttpServletResponse();
    StringWriter writer = new StringWriter();
    // MockHttpServletResponse already has a writer; we just call the filter
    filter.doFilterInternal(request, blocked, filterChain);

    verify(filterChain, times(5)).doFilter(any(), any());
    assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(blocked.getContentAsString()).contains("Too Many Requests");
    assertThat(blocked.getContentAsString()).contains("5 requests per minute");
  }

  @Test
  void differentIpsHaveIndependentBuckets() throws Exception {
    MockHttpServletRequest ip1 = new MockHttpServletRequest("POST", "/api/auth/login");
    ip1.setRemoteAddr("10.0.0.10");
    MockHttpServletRequest ip2 = new MockHttpServletRequest("POST", "/api/auth/login");
    ip2.setRemoteAddr("10.0.0.20");

    for (int i = 0; i < 5; i++) {
      filter.doFilterInternal(ip1, new MockHttpServletResponse(), filterChain);
      filter.doFilterInternal(ip2, new MockHttpServletResponse(), filterChain);
    }

    // both IPs still allowed on 5th (already counted above), 6th for each should be blocked
    MockHttpServletResponse blocked1 = new MockHttpServletResponse();
    filter.doFilterInternal(ip1, blocked1, filterChain);
    MockHttpServletResponse blocked2 = new MockHttpServletResponse();
    filter.doFilterInternal(ip2, blocked2, filterChain);

    assertThat(blocked1.getStatus()).isEqualTo(429);
    assertThat(blocked2.getStatus()).isEqualTo(429);
    verify(filterChain, times(10)).doFilter(any(), any()); // only the first 5+5
  }

  @Test
  void usesXForwardedForWhenPresent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setRemoteAddr("127.0.0.1"); // proxy
    request.addHeader("X-Forwarded-For", "203.0.113.50, 10.0.0.1");

    for (int i = 0; i < 5; i++) {
      filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);
    }

    MockHttpServletResponse blocked = new MockHttpServletResponse();
    filter.doFilterInternal(request, blocked, filterChain);

    assertThat(blocked.getStatus()).isEqualTo(429);
  }
}
