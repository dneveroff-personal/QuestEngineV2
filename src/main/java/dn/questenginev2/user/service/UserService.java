package dn.questenginev2.user.service;

import java.util.Optional;

import dn.questenginev2.auth.dto.ResetAdminPasswordRequest;
import dn.questenginev2.user.dto.ResetPasswordRequest;
import dn.questenginev2.user.dto.UserResponse;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import org.springframework.security.core.Authentication;

public interface UserService {

    User saveUser(User user);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    User getCurrentUser(Authentication auth);

    User getUser(Long userId);

    UserResponse setUserRole(Long userId, UserRole role, Authentication auth);

    void resetPassword(Long userId, ResetPasswordRequest request, Authentication auth);

    void resetAdminPassword(ResetAdminPasswordRequest request);
}
