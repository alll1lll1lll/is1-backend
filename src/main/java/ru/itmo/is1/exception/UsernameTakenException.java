package ru.itmo.is1.exception;

public class UsernameTakenException extends ApiException {
    public UsernameTakenException(String username) {
        super(ErrorCode.USERNAME_TAKEN, "Username '" + username + "' is already taken");
    }
}
