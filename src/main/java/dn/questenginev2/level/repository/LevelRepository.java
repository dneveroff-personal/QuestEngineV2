package dn.questenginev2.level.repository;

import dn.questenginev2.level.entity.Level;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LevelRepository
    extends JpaRepository<Level, Long>, JpaSpecificationExecutor<Level> {

  @Query("SELECT MAX(l.orderIndex) FROM Level l WHERE l.quest.id = :questId")
  Integer findMaxOrderIndex(@Param("questId") Long questId);

  @EntityGraph(attributePaths = {"quest"})
  List<Level> findByQuestIdOrderByOrderIndex(Long questId);

  Optional<Level> findByQuestIdAndOrderIndex(Long questId, int orderIndex);
}
