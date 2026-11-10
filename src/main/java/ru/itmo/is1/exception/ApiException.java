package ru.itmo.is1.exception;

import lombok.Getter;

@Getter
public abstract class ApiException extends RuntimeException {
    private final ErrorCode code;

    protected ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
