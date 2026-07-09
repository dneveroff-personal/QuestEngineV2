package dn.questenginev2.user.dto;

import dn.questenginev2.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String publicName;
    private String email;
    private UserRole role;
    private Instant createdAt;

}
