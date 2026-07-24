package dn.questenginev2.common.exceptions;

import org.springframework.security.authentication.BadCredentialsException;

public class WrongPasswordException extends BadCredentialsException {
  public WrongPasswordException(String message) {
    super(message);
  }
}
