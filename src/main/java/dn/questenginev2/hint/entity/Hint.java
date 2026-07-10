package dn.questenginev2.hint.entity;

import dn.questenginev2.level.entity.Level;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

import static jakarta.persistence.FetchType.LAZY;

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

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
