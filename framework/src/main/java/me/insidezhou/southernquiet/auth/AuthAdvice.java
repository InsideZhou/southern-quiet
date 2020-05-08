package me.insidezhou.southernquiet.auth;


import org.jetbrains.annotations.NotNull;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuthAdvice implements MethodBeforeAdvice {
    public final static String AuthorizationMatcherQualifier = "AuthAdvice.AuthorizationMatcherQualifier";

    private final PathMatcher pathMatcher;

    private AuthProvider authProvider;

    public AuthAdvice(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }

    @Override
    public void before(@NotNull Method method, @NotNull Object[] args, Object target) throws AuthException {
        Assert.notNull(target, "身份及权限验证时目标对象不该为null");

        Auth methodAuthorization = AnnotationUtils.getAnnotation(method, Auth.class);
        Auth classAuthorization = AnnotationUtils.getAnnotation(target.getClass(), Auth.class);

        Map<Auth.MatchMode, List<Auth>> groupedAuth = Stream.of(methodAuthorization, classAuthorization).filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Auth::mode));

        if (groupedAuth.isEmpty()) return;
        if (null == authProvider) throw new NoAuthProviderExistsException();

        Set<String> allPermissions = groupedAuth.getOrDefault(Auth.MatchMode.All, Collections.emptyList()).stream()
            .flatMap(auth -> Arrays.stream(auth.permissions()))
            .filter(permission -> !StringUtils.isEmpty(permission))
            .collect(Collectors.toSet());

        Set<String> anyPermissions = groupedAuth.getOrDefault(Auth.MatchMode.Any, Collections.emptyList()).stream()
            .flatMap(auth -> Arrays.stream(auth.permissions()))
            .filter(permission -> !StringUtils.isEmpty(permission))
            .collect(Collectors.toSet());

        if (allPermissions.isEmpty() && anyPermissions.isEmpty()) return;
        Authentication authentication = authProvider.getAuthentication(new AuthContext(method, args, target));
        final Set<String> patterns = authentication.getPermissionPatterns();

        boolean permissionCheckPassed = allPermissions.stream().allMatch(permission ->
            patterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, permission))
        );

        permissionCheckPassed = permissionCheckPassed && (anyPermissions.isEmpty() ||
            (!patterns.isEmpty() && anyPermissions.stream().anyMatch(permission ->
                patterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, permission))
            ))
        );

        if (!permissionCheckPassed) {
            throw new AuthorizationFailException(
                patterns,
                Stream.concat(allPermissions.stream(), anyPermissions.stream()).collect(Collectors.toSet())
            );
        }
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired(required = false)
    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }
}
