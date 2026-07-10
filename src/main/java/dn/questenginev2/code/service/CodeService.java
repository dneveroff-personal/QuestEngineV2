package dn.questenginev2.code.service;

import dn.questenginev2.code.dto.CreateCodeRequest;
import dn.questenginev2.code.dto.CodeResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface CodeService {

    CodeResponse createCode(Long levelId, CreateCodeRequest request, Authentication auth);

    List<CodeResponse> getCodesByLevelId(Long levelId);

    CodeResponse getCodeById(Long codeId);

    CodeResponse updateCode(Long codeId, CreateCodeRequest request, Authentication auth);

    void deleteCode(Long codeId);
}
