package dn.questenginev2.level.entity;

import static jakarta.persistence.FetchType.LAZY;

import dn.questenginev2.quest.entity.QuestProgress;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "level_progress")
public class LevelProgress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "quest_progress_id", nullable = false)
  private QuestProgress questProgress;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "level_id", nullable = false)
  private Level level;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private LevelProgressStatus status;

  @Column(name = "opened_at")
  private Instant openedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "auto_transition_at")
  private Instant autoTransitionAt;
}
