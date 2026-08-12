package dn.questenginev2.quest.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "quests")
public class Quest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "type", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private QuestType type = QuestType.TEAM;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private QuestStatus status = QuestStatus.DRAFT;

  @Column(name = "maximum_teams", nullable = false)
  @Builder.Default
  private Integer maximumTeams = 100;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  @Column(name = "started_at")
  private Instant startTime;

  @Column(name = "end_at")
  private Instant finishTime;
}
