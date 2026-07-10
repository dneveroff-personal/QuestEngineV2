package dn.questenginev2.level.repository;

import dn.questenginev2.level.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelRepository extends JpaRepository<Level, Long> {
}
