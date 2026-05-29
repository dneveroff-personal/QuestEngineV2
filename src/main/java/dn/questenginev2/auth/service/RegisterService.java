package dn.questenginev2.auth.service;

import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.auth.dto.RegisterRequest;

public interface RegisterService {

    void validateUserForRegistration(String username, String email);

    LoginResponse register(RegisterRequest dto);

}
