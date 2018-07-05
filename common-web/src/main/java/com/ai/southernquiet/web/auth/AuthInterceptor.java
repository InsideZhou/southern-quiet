package com.ai.southernquiet.web.auth;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class AuthInterceptor extends HandlerInterceptorAdapter {
    private AuthService authService;

    @Autowired
    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        Auth beanAuth = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), Auth.class);
        Auth methodAuth = handlerMethod.getMethodAnnotation(Auth.class);

        if (null == beanAuth && null == methodAuth) return true;

        Request req = Request.build(request, response, authService, getRequestClass());

        if (null == request.getRemoteUser()) {
            rebuildUserFromRememberCookie(req);
        }

        Set<String> authNames = new HashSet<>();
        Set<String> whiteRoles = new HashSet<>();
        Set<String> blackRoles = new HashSet<>();

        collectAuthData(authNames, whiteRoles, blackRoles, beanAuth);
        collectAuthData(authNames, whiteRoles, blackRoles, methodAuth);

        if (null == req.getRemoteUser()) {
            return onAuthenticationFail(req, response, handlerMethod);
        }

        if (!checkRoles(req, response, whiteRoles, blackRoles)) {
            return onAuthorizationFail(req, response, handlerMethod);
        }

        if (!checkAuthNames(req, response, authNames)) {
            return onAuthorizationFail(req, response, handlerMethod);
        }

        return true;
    }

    /**
     * 权限检查。
     *
     * @return 返回true时表示检查通过。
     */
    @SuppressWarnings("unused")
    protected boolean checkAuthNames(Request request, HttpServletResponse response, Set<String> authNames) {
        return authService.checkAuthorization(request.getRemoteUser(), authNames);
    }

    /**
     * 角色检查。
     *
     * @return 返回true时表示检查通过。
     */
    @SuppressWarnings("unused")
    protected boolean checkRoles(Request request, HttpServletResponse response, Set<String> white, Set<String> black) {
        Set<String> roles = request.getUserRoles();

        if (!white.isEmpty() && roles.stream().noneMatch(white::contains)) return false;

        if (!black.isEmpty() && roles.stream().anyMatch(black::contains)) return false;

        return true;
    }

    @SuppressWarnings("unused")
    protected boolean onAuthenticationFail(Request request, HttpServletResponse response, HandlerMethod handlerMethod) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "");
        return false;
    }

    @SuppressWarnings("unused")
    protected boolean onAuthorizationFail(Request request, HttpServletResponse response, HandlerMethod handlerMethod) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "");
        return false;
    }

    protected Class<? extends Request> getRequestClass() {
        return Request.class;
    }

    private void collectAuthData(Set<String> authNames, Set<String> whiteRoles, Set<String> blackRoles, Auth auth) {
        if (null != auth) {
            if (StringUtils.hasText(auth.name())) {
                authNames.add(auth.name());
            }

            Arrays.stream(auth.whiteRoles()).filter(StringUtils::hasText).forEach(whiteRoles::add);
            Arrays.stream(auth.blackRoles()).filter(StringUtils::hasText).forEach(blackRoles::add);
        }
    }

    private void rebuildUserFromRememberCookie(Request request) {
        Cookie[] cookies = request.getCookies();
        if (null == cookies) return;

        Optional<Cookie> opt = Arrays.stream(cookies).filter(c -> Request.KEY_REMEMBER_ME_COOKIE.equals(c.getName())).findFirst();
        if (opt.isPresent()) {
            User user = authService.getUserByRememberToken(opt.get().getValue());
            if (null != user) {
                request.writeUser(user);
            }
        }
    }
}
