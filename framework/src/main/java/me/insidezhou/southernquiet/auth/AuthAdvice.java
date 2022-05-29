package me.insidezhou.southernquiet.auth;


import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
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
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AuthAdvice.class);

    public final static String AuthorizationMatcherQualifier = "AuthAdvice.AuthorizationMatcherQualifier";

    private final BeanFactory beanFactory;

    private PathMatcher pathMatcher;
    private AuthProvider authProvider;
    private boolean initialized = false;

    public AuthAdvice(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    protected void initOnceBeforeWork() throws AuthException {
        pathMatcher = BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, PathMatcher.class, AuthorizationMatcherQualifier);
        try {
            if (null == authProvider) {
                authProvider = beanFactory.getBean(AuthProvider.class);
            }
        }
        catch (Exception e) {
            throw new NoAuthProviderExistsException();
        }
    }

    @Override
    public void before(@NotNull Method method, @NotNull Object[] args, Object target) throws AuthException {
        if (!initialized) {
            initOnceBeforeWork();
            initialized = true;
        }

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

        log.message("permission required")
            .context("all", allPermissions)
            .context("any", anyPermissions)
            .debug();

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
}
