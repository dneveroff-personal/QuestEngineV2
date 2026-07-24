package dn.questenginev2.team.repository;

import dn.questenginev2.team.entity.Team;
import dn.questenginev2.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

  boolean existsByName(String name);

  Optional<Team> findByCaptain(User captain);
}
