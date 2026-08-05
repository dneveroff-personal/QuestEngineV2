package dn.questenginev2.code.entity;

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
@Table(name = "codes")
public class Code {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "level_id", nullable = false)
  private Level level;

  @Column(name = "code_value", nullable = false)
  private String value;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private CodeType type;

  @Column(name = "points")
  private Integer points;

  @Builder.Default
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
