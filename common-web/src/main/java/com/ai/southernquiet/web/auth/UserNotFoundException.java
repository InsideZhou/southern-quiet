package com.ai.southernquiet.web.auth;

public class UserNotFoundException extends AuthException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
