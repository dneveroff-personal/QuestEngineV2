package dn.questenginev2.team.repository;

import dn.questenginev2.team.entity.Team;
import dn.questenginev2.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByName(String name);

    Optional<Team> findByCaptain(User captain);

}
