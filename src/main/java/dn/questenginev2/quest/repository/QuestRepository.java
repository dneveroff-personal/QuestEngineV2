package dn.questenginev2.quest.repository;

import dn.questenginev2.quest.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestRepository extends JpaRepository<Quest, Long> {
    Optional<Quest> findByTitle(String title);
}
