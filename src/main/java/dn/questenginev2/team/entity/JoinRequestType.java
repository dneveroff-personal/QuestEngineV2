package dn.questenginev2.team.entity;

import dn.questenginev2.common.exceptions.UserNotFoundException;
import dn.questenginev2.user.entity.User;
import dn.questenginev2.user.service.UserService;
import org.springframework.security.core.Authentication;

public enum JoinRequestType {
    JOIN_REQUEST,
    CAPTAIN_INVITE;

    public static JoinRequestType of(String username) {
        return (username != null && !username.isBlank()) ? CAPTAIN_INVITE : JOIN_REQUEST;
    }

    public User resolveUser(UserService userService, String username, User currentUser) {
        if (this == CAPTAIN_INVITE) {
            return userService.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException("Приглашаемый пользователь не найден: " + username));
        }
        return currentUser;
    }
}
