package com.ai.southernquiet.web.auth;

import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

public class Request extends HttpServletRequestWrapper {
    private HttpServletResponse response;
    private AuthService authService;
    private CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties;
    private CommonWebAutoConfiguration.WebProperties webProperties;

    private HttpSession session;

    public Request(HttpServletRequest request,
                   HttpServletResponse response,
                   CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties,
                   CommonWebAutoConfiguration.WebProperties webProperties,
                   AuthService authService) {

        super(request);

        session = request.getSession(); //提前初始化session，避免“java.lang.IllegalStateException: Cannot create a session after the response has been committed”。

        this.authService = authService;
        this.response = response;
        this.rememberMeProperties = rememberMeProperties;
        this.webProperties = webProperties;
    }

    @Override
    public String getAuthType() {
        User<?> user = getUser();

        return null == user ? null : HttpServletRequest.FORM_AUTH;
    }

    @Override
    public String getRemoteUser() {
        User<?> user = getUser();
        if (null == user) return null;

        return user.getAccount().getName();
    }

    @Override
    public boolean isUserInRole(String role) {
        return getUserRoles().contains(role);
    }

    @Override
    public Principal getUserPrincipal() {
        User<?> user = getUser();
        if (null == user) return null;

        return user.getAccount();
    }

    public void authenticate() throws ServletException {
        User<?> user;
        try {
            user = authService.authenticate(this);
        }
        catch (AuthException e) {
            throw new ServletException(e);
        }

        if (null == user) throw new ServletException(new AuthException("由于未知原因，身份验证失败"));

        writeUser(user);
        writeRememberMeCookie(user.getRememberToken());
    }

    @Override
    public void login(String username, String password) throws ServletException {
        if (getAuthType() != null || getRemoteUser() != null || getUserPrincipal() != null) {
            throw new ServletException("已经登录过了。");
        }

        try {
            User<?> user = login(username, password, true);
            if (null == user) throw new AuthException("由于未知原因，身份验证失败");
        }
        catch (AuthException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void logout() {
        session.removeAttribute(webProperties.getUser());

        writeRememberMeCookie("");
    }

    public User<?> getUser() {
        Object u = session.getAttribute(webProperties.getUser());

        if (null == u || !User.class.isAssignableFrom(u.getClass())) {
            return null;
        }

        return (User<?>) u;
    }

    public Set<String> getUserRoles() {
        User<?> user = getUser();
        if (null == user) return new HashSet<>();

        return user.getRoles();
    }

    public User<?> login(String username, String password, boolean remember) throws AuthException {
        User<?> user = authService.authenticate(username, password, remember);
        if (null == user) throw new AuthException("由于未知原因，身份验证失败");

        writeUser(user);
        writeRememberMeCookie(user.getRememberToken());

        return user;
    }

    protected void writeUser(User<?> user) {
        session.setAttribute(webProperties.getUser(), user);
    }

    /**
     * 写入remember_me cookie。当 {@param token}为空时，删除该cookie。
     */
    protected void writeRememberMeCookie(String token) {
        Cookie cookie = new Cookie(rememberMeProperties.getCookie(), token);
        cookie.setHttpOnly(true);

        if (StringUtils.hasLength(token)) {
            cookie.setMaxAge((int) rememberMeProperties.getTimeout().getSeconds());
        }
        else {
            cookie.setMaxAge(0);
        }

        response.addCookie(cookie);
    }
}
