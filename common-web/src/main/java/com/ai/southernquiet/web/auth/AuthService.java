package com.ai.southernquiet.web.auth;

import java.util.Set;

public interface AuthService {
    /**
     * 验证用户身份。
     *
     * @param username 用户名
     * @param password 密码
     * @param remember 记住该用户。如果true，返回的 {@link User#getRememberToken()} 不为空。
     * @throws UserNotFoundException      找不到用户
     * @throws IncorrectPasswordException 密码不正确
     */
    User<?> authenticate(String username, String password, boolean remember) throws AuthException;

    /**
     * 验证用户身份。
     */
    <R extends Request> User<?> authenticate(R request) throws AuthException;

    /**
     * 通过remember token的方式获取用户。
     * <p>此时{@link User#isAuthenticated()}应为false。</p>
     */
    User<?> getUserByRememberToken(String token);

    /**
     * 检查用户是否具备所有指定的权限。
     *
     * @param username  用户名
     * @param authNames 权限集合
     */
    boolean checkAuthorization(String username, Set<String> authNames);
}