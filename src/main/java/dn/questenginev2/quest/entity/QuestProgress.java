package dn.questenginev2.quest.entity;

import dn.questenginev2.team.entity.Team;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
    name = "quest_progress",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"quest_id", "team_id"})})
public class QuestProgress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "quest_id", nullable = false)
  private Quest quest;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private QuestProgressStatus status = QuestProgressStatus.WAITING;

  @Column(name = "quest_started_at", nullable = false)
  private Instant questStartedAt;

  @Column(name = "entered_at")
  private Instant enteredAt;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  @Column(name = "finished_at")
  private Instant finishedAt;
}
