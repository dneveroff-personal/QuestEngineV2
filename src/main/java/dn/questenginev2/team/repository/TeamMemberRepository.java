package dn.questenginev2.team.repository;

import dn.questenginev2.team.entity.TeamMember;
import dn.questenginev2.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    boolean existsByUser(User user);

}
