package dn.questenginev2.auth.service;

import dn.questenginev2.auth.dto.AuthRequestBase;
import dn.questenginev2.auth.dto.LoginRequest;
import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.user.entity.User;

public interface LoginService {

    LoginResponse login(AuthRequestBase request);
    LoginResponse login(AuthRequestBase request, User user);

}