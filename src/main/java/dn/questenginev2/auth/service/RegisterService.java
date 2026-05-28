package dn.questenginev2.auth.service;

import dn.questenginev2.auth.dto.RegisterRequest;
import dn.questenginev2.auth.dto.RegisterResponse;

public interface RegisterService {

    RegisterResponse register(RegisterRequest dto);

}
