package dn.questenginev2.quest.service;

import dn.questenginev2.quest.dto.CurrentLevelResponse;
import org.springframework.security.core.Authentication;

public interface CurrentLevelService {

  /**
   * Active level for the team on the quest: LevelProgress + level content + hints + main-code
   * progress. Throws if no ACTIVE LevelProgress.
   */
  CurrentLevelResponse getCurrentLevel(Long questId, Long teamId, Authentication auth);
}
