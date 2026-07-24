package dn.questenginev2.code.repository;

import dn.questenginev2.code.entity.Code;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeRepository extends JpaRepository<Code, Long> {

  @Query(
      "SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Code c WHERE c.value ="
          + " :codeValue")
  boolean existsByCodeValue(@Param("codeValue") String codeValue);

  List<Code> findByLevelIdOrderByCreatedAt(Long levelId);
}
