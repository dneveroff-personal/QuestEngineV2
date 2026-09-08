package dn.questenginev2.bonuspenalty.service;

import dn.questenginev2.bonuspenalty.dto.CreateManualTimeAdjustmentRequest;
import dn.questenginev2.bonuspenalty.dto.ManualTimeAdjustmentResponse;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface ManualTimeAdjustmentService {

  ManualTimeAdjustmentResponse create(
      Long questProgressId, CreateManualTimeAdjustmentRequest request, Authentication auth);

  ManualTimeAdjustmentResponse revoke(Long adjustmentId, Authentication auth);

  List<ManualTimeAdjustmentResponse> getByQuestProgressId(Long questProgressId);
}
