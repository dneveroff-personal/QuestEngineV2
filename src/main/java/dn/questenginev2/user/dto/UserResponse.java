package dn.questenginev2.user.dto;

import dn.questenginev2.user.entity.UserRole;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {

  private Long id;
  private String publicName;
  private String email;
  private UserRole role;
  private Instant createdAt;
}
