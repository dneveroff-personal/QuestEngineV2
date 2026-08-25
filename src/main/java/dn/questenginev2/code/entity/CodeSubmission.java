package dn.questenginev2.code.entity;

import static jakarta.persistence.FetchType.LAZY;

import dn.questenginev2.level.entity.LevelProgress;
import dn.questenginev2.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Факт попытки команды ввести код на активном уровне. Каждая попытка — отдельная запись,
 * включая неверные попытки (аудит, см. 01-domain/code-submission.md).
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "code_submissions")
public class CodeSubmission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "level_progress_id", nullable = false)
  private LevelProgress levelProgress;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "submitted_by", nullable = false)
  private User submittedBy;

  @Column(name = "raw_value", nullable = false)
  private String rawValue;

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "matched_code_id")
  private Code matchedCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "result", nullable = false)
  private CodeSubmissionResult result;

  @Builder.Default
  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt = Instant.now();
}
