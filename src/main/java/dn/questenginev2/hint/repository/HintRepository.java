package dn.questenginev2.hint.repository;

import dn.questenginev2.hint.entity.Hint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HintRepository extends JpaRepository<Hint, Long> {

    @Query("SELECT MAX(h.orderIndex) FROM Hint h WHERE h.level.id = :levelId")
    Integer findMaxOrderIndex(@Param("levelId") Long levelId);

    List<Hint> findByLevelIdOrderByOrderIndex(Long levelId);
}
