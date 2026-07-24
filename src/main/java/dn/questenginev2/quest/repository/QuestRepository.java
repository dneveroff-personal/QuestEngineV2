package dn.questenginev2.quest.repository;

import dn.questenginev2.quest.entity.Quest;
import dn.questenginev2.quest.entity.QuestStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestRepository extends JpaRepository<Quest, Long> {

  List<Quest> findByStatus(QuestStatus status);

  List<Quest> findByStatus(QuestStatus status, Pageable pageable);
}
