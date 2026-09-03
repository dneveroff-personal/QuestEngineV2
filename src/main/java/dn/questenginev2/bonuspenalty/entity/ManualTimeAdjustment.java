package dn.questenginev2.bonuspenalty.entity;

import dn.questenginev2.quest.entity.QuestProgress;
import dn.questenginev2.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "manual_time_adjustments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualTimeAdjustment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "quest_progress_id", nullable = false)
  private QuestProgress questProgress;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TimeAdjustmentType type;

  @Column(nullable = false)
  private Integer seconds;

  @Column(nullable = false, length = 1000)
  private String reason;

  @ManyToOne(optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant revokedAt;

  @ManyToOne
  @JoinColumn(name = "revoked_by")
  private User revokedBy;
}
