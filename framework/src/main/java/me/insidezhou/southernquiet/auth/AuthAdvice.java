package me.insidezhou.southernquiet.auth;


import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuthAdvice implements MethodBeforeAdvice {
    private final static Logger log = LoggerFactory.getLogger(AuthAdvice.class);

    public final static String AuthorizationMatcherQualifier = "AuthAdvice.AuthorizationMatcherQualifier";

    private final PathMatcher pathMatcher;

    private AuthProvider authProvider;

    public AuthAdvice(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }

    @Override
    public void before(@NotNull Method method, @NotNull Object[] args, Object target) throws AuthException {
        Assert.notNull(target, "身份及权限验证时目标对象不该为null");

        Auth methodAuthorization = AnnotatedElementUtils.findMergedAnnotation(method, Auth.class);
        Auth classAuthorization = AnnotatedElementUtils.findMergedAnnotation(target.getClass(), Auth.class);

        Map<Auth.MatchMode, Set<String>> groupedPermissions = Stream.of(methodAuthorization, classAuthorization).filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Auth::mode))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().flatMap(auth -> Arrays.stream(auth.permissions())).collect(Collectors.toSet())
            ));

        if (groupedPermissions.isEmpty()) return;
        if (null == authProvider) throw new NoAuthProviderExistsException();

        groupedPermissions = groupedPermissions.entrySet().stream()
            .filter(entry -> !CollectionUtils.isEmpty(entry.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));

        Set<String> allPermissions = groupedPermissions.getOrDefault(Auth.MatchMode.All, authProvider.getDefaultPermissionsForAllMode()).stream()
            .filter(permission -> !StringUtils.isEmpty(permission))
            .collect(Collectors.toSet());

        Set<String> anyPermissions = groupedPermissions.getOrDefault(Auth.MatchMode.Any, authProvider.getDefaultPermissionsForAnyMode()).stream()
            .filter(permission -> !StringUtils.isEmpty(permission))
            .collect(Collectors.toSet());

        if (allPermissions.isEmpty() && anyPermissions.isEmpty()) return;

        if (log.isDebugEnabled()) {
            log.debug("permission required\tall={}, any={}", allPermissions, anyPermissions);
        }

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
