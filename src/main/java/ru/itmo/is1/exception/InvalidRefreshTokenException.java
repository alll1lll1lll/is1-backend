package ru.itmo.is1.exception;

public class InvalidRefreshTokenException extends ApiException {
    public InvalidRefreshTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token is invalid, expired or revoked");
    }
}
