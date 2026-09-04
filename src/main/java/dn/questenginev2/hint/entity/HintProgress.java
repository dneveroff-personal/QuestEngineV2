package dn.questenginev2.hint.entity;

import static jakarta.persistence.FetchType.LAZY;

import dn.questenginev2.level.entity.LevelProgress;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Факт показа подсказки команде (auto-reveal, ADR-0020). В отличие от CodeSubmission, здесь нет
 * "неудачных попыток" — показ либо произошёл (есть запись), либо ещё не произошёл (записи нет).
 * Не хранит openedBy — показ не является ручным действием участника.
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "hint_progress")
public class HintProgress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "level_progress_id", nullable = false)
  private LevelProgress levelProgress;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "hint_id", nullable = false)
  private Hint hint;

  @Builder.Default
  @Column(name = "shown_at", nullable = false)
  private Instant shownAt = Instant.now();
}
