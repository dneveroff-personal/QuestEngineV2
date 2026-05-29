package dn.questenginev2.user.service;

import java.util.Optional;

import dn.questenginev2.user.entity.User;

public interface UserService {

    User saveUser(User user);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

}
