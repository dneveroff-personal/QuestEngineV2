package dn.questenginev2.user.service;

import java.util.Optional;

import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void validateUserForRegistration(String username, String email) {
        if (existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (email != null && !email.isBlank() && existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    private boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    private boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
