package dn.questenginev2.level.repository;

import dn.questenginev2.level.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LevelRepository extends JpaRepository<Level, Long> {

    @Query("SELECT MAX(l.orderIndex) FROM Level l WHERE l.quest.id = :questId")
    Integer findMaxOrderIndex(@Param("questId") Long questId);

    List<Level> findByQuestIdOrderByOrderIndex(Long questId);
}
