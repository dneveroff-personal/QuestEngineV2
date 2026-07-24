package dn.questenginev2.level.entity;

import static jakarta.persistence.FetchType.LAZY;

import dn.questenginev2.quest.entity.Quest;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "levels")
public class Level {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "quest_id", nullable = false)
  private Quest quest;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "order_idx", nullable = false)
  private Integer orderIndex;

  @Lob
  @Basic(fetch = LAZY)
  private String content;

  @Column(name = "timeout")
  private Integer timeoutSeconds;

  @Builder.Default private Instant createdAt = Instant.now();

  @Builder.Default private Instant updatedAt = Instant.now();
}
