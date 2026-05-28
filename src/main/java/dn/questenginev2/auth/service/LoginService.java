package dn.questenginev2.auth.service;

import dn.questenginev2.auth.dto.LogInRequest;
import dn.questenginev2.auth.dto.LoginResponse;

public interface LoginService {

    LoginResponse login(LogInRequest request);

}