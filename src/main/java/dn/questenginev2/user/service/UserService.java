package dn.questenginev2.user.service;

import java.util.Optional;

import dn.questenginev2.user.entity.User;

public interface UserService {

    void validateUserForRegistration(String username, String email);

    User saveUser(User user);

    Optional<User> findByUsername(String username);

}
