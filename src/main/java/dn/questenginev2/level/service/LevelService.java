package dn.questenginev2.level.service;

import dn.questenginev2.level.dto.CreateLevelRequest;
import dn.questenginev2.level.dto.LevelResponse;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface LevelService {

  LevelResponse createLevel(Long questId, CreateLevelRequest request, Authentication auth);

  List<LevelResponse> getLevelsByQuestId(Long questId);

  LevelResponse getLevelById(Long levelId);

  LevelResponse updateLevel(Long levelId, CreateLevelRequest request, Authentication auth);

  void deleteLevel(Long levelId, Authentication auth);

  Integer getMaxLevelIndex(Long questId);
}
