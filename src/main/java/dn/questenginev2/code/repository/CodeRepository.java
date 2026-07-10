package dn.questenginev2.code.repository;

import dn.questenginev2.code.entity.Code;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeRepository extends JpaRepository<Code, Long> {

    boolean existsByValue(String value);

    List<Code> findByLevelIdOrderByCreatedAt(Long levelId);
}
