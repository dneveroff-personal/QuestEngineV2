package dn.questenginev2.code.service;

import dn.questenginev2.code.dto.CodeSubmissionResponse;
import dn.questenginev2.code.dto.SubmitCodeRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

public interface CodeSubmissionService {

  // Ввод кода любым участником команды на активном уровне (01-domain/code-submission.md)
  CodeSubmissionResponse submitCode(
      Long questId, Long teamId, @Valid SubmitCodeRequest request, Authentication auth);
}
