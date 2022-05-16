package test.auth;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.auth.*;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.PathMatcher;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static me.insidezhou.southernquiet.auth.AuthAdvice.AuthorizationMatcherQualifier;

@SpringBootTest(classes = {FrameworkAutoConfiguration.class, AuthTest.Config.class})
@ExtendWith(SpringExtension.class)
public class AuthTest {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AuthTest.class);

    @Configuration
    public static class Config {
        @Bean
        public SecurityTarget securityTarget() {
            return new SecurityTarget();
        }
    }

    @Autowired
    private SecurityTarget securityTarget;

    @Autowired
    private AuthAdvice authAdvice;

    @Autowired
    @Qualifier(AuthorizationMatcherQualifier)
    private PathMatcher pathMatcher;

    @Test
    public void noAuthProvider() {
        authAdvice.setAuthProvider(null);

        NoAuthProviderExistsException noAuthProviderExistsException = null;

        try {
            securityTarget.securityMethod1();
        }
        catch (UndeclaredThrowableException e) {
            noAuthProviderExistsException = (NoAuthProviderExistsException) e.getCause();
        }

        Assertions.assertNotNull(noAuthProviderExistsException);
    }

    @Test
    public void auth() {
        authAdvice.setAuthProvider(context -> new Authentication() {
            @NotNull
            @Override
            public String getId() {
                return UUID.randomUUID().toString();
            }

            @NotNull
            @Override
            public Set<String> getPermissionPatterns() {
                return Collections.emptySet();
            }
        });

        securityTarget.securityMethod1();
    }

    @Test
    public void authorize() {
        authAdvice.setAuthProvider(context -> new Authentication() {
            @NotNull
            @Override
            public String getId() {
                return UUID.randomUUID().toString();
            }

            @NotNull
            @Override
            public Set<String> getPermissionPatterns() {
                return Collections.emptySet();
            }
        });

        AuthorizationFailException authorizationFailException = null;

        try {
            securityTarget.securityMethod2();
        }
        catch (UndeclaredThrowableException e) {
            authorizationFailException = (AuthorizationFailException) e.getCause();
        }

        Assertions.assertNotNull(authorizationFailException);

        authAdvice.setAuthProvider(context -> new Authentication() {
            @NotNull
            @Override
            public String getId() {
                return UUID.randomUUID().toString();
            }

            @NotNull
            @Override
            public Set<String> getPermissionPatterns() {
                return Collections.singleton("admin/*");
            }
        });

        securityTarget.securityMethod2();
    }

    @Test
    public void antPath() {
        Assertions.assertTrue(pathMatcher.match("security/*", "security/allCredentials"));
        Assertions.assertTrue(pathMatcher.match("security/*", "security/"));

        Assertions.assertFalse(pathMatcher.match("security/*", "security"));
        Assertions.assertFalse(pathMatcher.match("security/*", "allCredentials"));
    }

    public static class SecurityTarget {
        @Auth
        public void securityMethod1() {
            log.message("securityMethod1 worked").context("permissionRequired", "<default>").debug();
        }

        @Auth("admin/credentials")
        public void securityMethod2() {
            log.message("securityMethod2 worked").context("permissionRequired", "admin/credentials").info();
        }
    }
}
