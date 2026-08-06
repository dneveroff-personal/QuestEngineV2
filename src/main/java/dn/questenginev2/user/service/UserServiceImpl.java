package dn.questenginev2.user.service;

import dn.questenginev2.auth.dto.ResetAdminPasswordRequest;
import dn.questenginev2.common.exceptions.ForbiddenOperationException;
import dn.questenginev2.common.exceptions.UserNotFoundException;
import dn.questenginev2.user.dto.ResetPasswordRequest;
import dn.questenginev2.user.dto.UserFilterRequest;
import dn.questenginev2.user.dto.UserResponse;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.entity.UserRole;
import dn.questenginev2.user.repository.UserRepository;
import dn.questenginev2.user.specification.UserSpecification;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  // ────── IMPLEMENTATIONS ───────────────────────────────────────────────────────────
  @Override
  public User saveUser(User user) {
    return userRepository.save(user);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public User getCurrentUser(Authentication auth) {
    String userName = auth.getName();
    return findByUsername(userName)
        .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + userName));
  }

  @Override
  public User getUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + userId));
  }

  @Override
  public UserResponse setUserRole(Long userId, UserRole role, Authentication auth) {
    User currentUser = getCurrentUser(auth);
    validateAdmin(currentUser);

    User targetUser = getUser(userId);
    targetUser.setRole(role);
    return buildUserResponse(userRepository.save(targetUser));
  }

  @Override
  public void resetPassword(Long userId, ResetPasswordRequest request, Authentication auth) {
    User currentUser = getCurrentUser(auth);
    validateAdmin(currentUser);

    User targetUser = getUser(userId);
    String encodedPassword = passwordEncoder.encode(request.newPassword());
    targetUser.setPasswordHash(encodedPassword);
    userRepository.save(targetUser);
  }

  @Override
  public void resetAdminPassword(ResetAdminPasswordRequest request) {
    User adminUser = getUser(1L);
    String encodedPassword = passwordEncoder.encode(request.newPassword());
    adminUser.setPasswordHash(encodedPassword);
    userRepository.save(adminUser);
  }

  @Override
  public List<UserResponse> searchUsers(UserFilterRequest filter, Pageable pageable) {
    var spec =
        Specification.where(UserSpecification.hasUsername(filter.username()))
            .and(UserSpecification.hasEmail(filter.email()))
            .and(UserSpecification.hasRole(filter.role()))
            .and(UserSpecification.createdAtAfter(filter.createdAtAfter()))
            .and(UserSpecification.createdAtBefore(filter.createdAtBefore()));

    return userRepository.findAll(spec, pageable).stream()
        .map(this::buildUserResponse)
        .collect(Collectors.toList());
  }

  // ────── VALIDATIONS ───────────────────────────────────────────────────────────
  private void validateAdmin(User currentUser) {
    if (currentUser.getRole() != UserRole.ADMIN) {
      throw new ForbiddenOperationException("Данная операция разрешена только Администратору");
    }
  }

  public boolean existsByUsername(String username) {
    return userRepository.findByUsername(username).isPresent();
  }

  public boolean existsByEmail(String email) {
    return userRepository.findByEmail(email).isPresent();
  }

  // ────── BUILDERS ───────────────────────────────────────────────────────────
  private UserResponse buildUserResponse(User user) {
    return new UserResponse(
        user.getId(), user.getPublicName(), user.getEmail(), user.getRole(), user.getCreatedAt());
  }
}
