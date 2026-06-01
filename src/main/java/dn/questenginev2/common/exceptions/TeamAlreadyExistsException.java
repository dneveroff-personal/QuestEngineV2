package dn.questenginev2.common.exceptions;

public class TeamAlreadyExistsException extends RequestAlreadyExistsException {
    public TeamAlreadyExistsException(String message) {
        super(message);
    }
}