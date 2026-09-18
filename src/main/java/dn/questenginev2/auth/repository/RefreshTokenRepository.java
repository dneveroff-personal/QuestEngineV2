package dn.questenginev2.auth.repository;

import dn.questenginev2.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);

  @Modifying
  @Query(
      "UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.user.id = :userId AND t.revokedAt IS"
          + " NULL")
  int revokeAllActiveForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
