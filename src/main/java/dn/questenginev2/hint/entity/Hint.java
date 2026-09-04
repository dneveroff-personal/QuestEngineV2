package dn.questenginev2.hint.entity;

import static jakarta.persistence.FetchType.LAZY;

import dn.questenginev2.level.entity.Level;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "hints")
public class Hint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "level_id", nullable = false)
  private Level level;

  @Column(name = "order_idx", nullable = false)
  private Integer orderIndex;

  @Column(name = "delay_seconds", nullable = false)
  private Integer delaySeconds;

  @Lob
  @Basic(fetch = LAZY)
  @Column(name = "content", nullable = false)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private HintType type;

  // Требуется для BONUS/PENALTY, недопустимо для REGULAR (см. HintServiceImpl.validateHintData).
  @Column(name = "bonus_penalty_seconds")
  private Integer bonusPenaltySeconds;

  @Builder.Default
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Builder.Default
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
