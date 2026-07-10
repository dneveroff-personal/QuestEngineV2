package dn.questenginev2.level.service;

import dn.questenginev2.level.dto.CreateLevelRequest;
import dn.questenginev2.level.dto.LevelResponse;
import org.springframework.security.core.Authentication;

public interface LevelService {

    LevelResponse createLevel(Long questId, CreateLevelRequest request, Authentication auth);
}
