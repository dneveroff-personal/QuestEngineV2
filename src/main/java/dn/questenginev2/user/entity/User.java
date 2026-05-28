package dn.questenginev2.user.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(length = 128)
    private String publicName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.PLAYER;

    private Instant createdAt = Instant.now();

}
