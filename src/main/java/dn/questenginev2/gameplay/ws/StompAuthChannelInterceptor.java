package dn.questenginev2.gameplay.ws;

import dn.questenginev2.auth.service.JwtService;
import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.quest.repository.QuestProgressRepository;
import dn.questenginev2.team.repository.TeamMemberRepository;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * STOMP CONNECT + SUBSCRIBE authorization (ADR-022, docs/frontend/realtime.md §11).
 *
 * <ul>
 *   <li>CONNECT: JWT from {@code Authorization: Bearer ...} or {@code access_token} header
 *   <li>SUBSCRIBE to {@code /topic/quest-progress/{id}/gameplay}: team member or ADMIN
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private static final Pattern QUEST_PROGRESS_DEST =
      Pattern.compile("^/topic/quest-progress/(\d+)/gameplay$");

  private final JwtService jwtService;
  private final UserRepository userRepository;
  private final QuestProgressRepository questProgressRepository;
  private final TeamMemberRepository teamMemberRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null) {
      return message;
    }

    StompCommand command = accessor.getCommand();
    if (StompCommand.CONNECT.equals(command)) {
      authenticateConnect(accessor);
    } else if (StompCommand.SUBSCRIBE.equals(command)) {
      authorizeSubscribe(accessor);
    }
    return message;
  }

  private void authenticateConnect(StompHeaderAccessor accessor) {
    String token = resolveToken(accessor);
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("STOMP CONNECT requires JWT");
    }
    try {
      Claims claims =
          Jwts.parser()
              .verifyWith(jwtService.getKey())
              .build()
              .parseSignedClaims(token)
              .getPayload();
      String username = claims.getSubject();
      String role = claims.get("role", String.class);
      if (username == null) {
        throw new IllegalArgumentException("JWT subject missing");
      }
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(
              username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
      accessor.setUser(auth);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JWT for STOMP CONNECT", e);
    }
  }

  private void authorizeSubscribe(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null) {
      return;
    }
    Matcher matcher = QUEST_PROGRESS_DEST.matcher(destination);
    if (!matcher.matches()) {
      // Other destinations (e.g. /topic/quests/{id}/gameplay) — deny until implemented
      if (destination.startsWith("/topic/")) {
        throw new IllegalArgumentException("Subscription not allowed: " + destination);
      }
      return;
    }
    long questProgressId = Long.parseLong(matcher.group(1));
    Object principal = accessor.getUser() != null ? accessor.getUser().getName() : null;
    if (principal == null) {
      throw new IllegalArgumentException("Not authenticated");
    }
    User user =
        userRepository
            .findByUsername(principal.toString())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (user.getRole() == UserRole.ADMIN) {
      return;
    }
    QuestProgress progress =
        questProgressRepository
            .findById(questProgressId)
            .orElseThrow(() -> new IllegalArgumentException("QuestProgress not found"));
    if (teamMemberRepository.findByUserAndTeam(user, progress.getTeam()).isEmpty()) {
      throw new IllegalArgumentException("Not a member of this team progress");
    }
  }

  private String resolveToken(StompHeaderAccessor accessor) {
    String auth = accessor.getFirstNativeHeader("Authorization");
    if (auth != null && auth.startsWith("Bearer ")) {
      return auth.substring(7);
    }
    String accessToken = accessor.getFirstNativeHeader("access_token");
    if (accessToken != null && !accessToken.isBlank()) {
      return accessToken;
    }
    // Native STOMP clients sometimes put token in connect headers map
    Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
    if (sessionAttrs != null && sessionAttrs.get("access_token") instanceof String t) {
      return t;
    }
    return null;
  }
}
