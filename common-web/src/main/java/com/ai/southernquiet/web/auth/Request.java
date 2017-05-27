package com.ai.southernquiet.web.auth;

import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

public class Request extends HttpServletRequestWrapper {
    static String KEY_USER = "com.ai.southernquiet.web.auth.User"; //User在session中的key
    static String KEY_REMEMBER_ME_COOKIE = "remember_me"; //记住我的cookie名称
    static int REMEMBER_ME_TIMEOUT = 31536000; //记住我的cookie有效时间，单位：秒

    private AuthService authService;
    private HttpServletResponse response;

    public Request(HttpServletRequest request, HttpServletResponse response, AuthService authService) {
        super(request);

        this.response = response;
        this.authService = authService;
    }

    @Override
    public String getAuthType() {
        return HttpServletRequest.FORM_AUTH;
    }

    @Override
    public String getRemoteUser() {
        User user = getUser();
        if (null == user) return null;

        return user.getUsername();
    }

    @Override
    public boolean isUserInRole(String role) {
        return getUserRoles().contains(role);
    }

    @Override
    public Principal getUserPrincipal() {
        User user = getUser();
        if (null == user) return null;

        return user::getUsername;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        login(getParameter("username"), getParameter("password"));
        return true;
    }

    @Override
    public void login(String username, String password) throws ServletException {
        try {
            login(username, password, false);
        }
        catch (AuthException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void logout() throws ServletException {
        HttpSession session = getSession();
        session.removeAttribute(KEY_USER);

        writeRememberMeCookie("");
    }

    public User getUser() {
        HttpSession session = getSession();
        Object u = session.getAttribute(KEY_USER);

        if (null == u || !User.class.isAssignableFrom(u.getClass())) {
            return null;
        }

        return (User) u;
    }

    public Set<String> getUserRoles() {
        User user = getUser();
        if (null == user) return new HashSet<>();

        return user.getRoles();
    }

    public void login(String username, String password, boolean remember) throws AuthException {
        User user = authService.authenticate(username, password, remember);
        writeUser(user);
        writeRememberMeCookie(user.getRememberToken());
    }

    public void writeUser(User user) {
        HttpSession session = getSession();
        session.setAttribute(KEY_USER, user);
    }

    /**
     * 写入remember_me cookie。当 {@param token}为空时，删除该cookie。
     */
    protected void writeRememberMeCookie(String token) {
        Cookie cookie = new Cookie(KEY_REMEMBER_ME_COOKIE, token);
        cookie.setHttpOnly(true);

        if (StringUtils.hasLength(token)) {
            cookie.setMaxAge(REMEMBER_ME_TIMEOUT);
        }
        else {
            cookie.setMaxAge(0);
        }

        response.addCookie(cookie);
    }
}
