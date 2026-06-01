package dn.questenginev2.common.exceptions;

public class UserAlreadyExistsException extends RequestAlreadyExistsException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}