package me.insidezhou.southernquiet.auth;

import java.util.Set;

public class AuthorizationFailException extends AuthException {
    private Set<String> permissions;
    private Set<String> required;

    public AuthorizationFailException() {}

    public AuthorizationFailException(Set<String> permissions, Set<String> required) {
        this.permissions = permissions;
        this.required = required;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Set<String> getRequired() {
        return required;
    }

    public void setRequired(Set<String> required) {
        this.required = required;
    }
}
