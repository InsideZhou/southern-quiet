package com.ai.southernquiet.web.auth;

public class IncorrectPasswordException extends AuthException {
    public IncorrectPasswordException(String message) {
        super(message);
    }

    public IncorrectPasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
