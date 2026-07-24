package dn.questenginev2.hint.service;

import dn.questenginev2.hint.dto.CreateHintRequest;
import dn.questenginev2.hint.dto.HintResponse;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface HintService {

  HintResponse createHint(Long levelId, CreateHintRequest request, Authentication auth);

  List<HintResponse> getHintsByLevelId(Long levelId);

  HintResponse getHintById(Long hintId);

  HintResponse updateHint(Long hintId, CreateHintRequest request, Authentication auth);

  void deleteHint(Long hintId, Authentication auth);

  Integer getMaxHintIndex(Long levelId);
}
