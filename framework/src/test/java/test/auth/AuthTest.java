package test.auth;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.auth.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.PathMatcher;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static me.insidezhou.southernquiet.auth.AuthAdvice.AuthorizationMatcherQualifier;

@SpringBootTest(classes = {FrameworkAutoConfiguration.class, AuthTest.Config.class})
@RunWith(SpringRunner.class)
public class AuthTest {
    private final static Logger log = LoggerFactory.getLogger(AuthTest.class);

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

        Assert.assertNotNull(noAuthProviderExistsException);
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

        Assert.assertNotNull(authorizationFailException);

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
        Assert.assertTrue(pathMatcher.match("security/*", "security/allCredentials"));
        Assert.assertTrue(pathMatcher.match("security/*", "security/"));

        Assert.assertFalse(pathMatcher.match("security/*", "security"));
        Assert.assertFalse(pathMatcher.match("security/*", "allCredentials"));
    }

    public static class SecurityTarget {
        @Auth
        public void securityMethod1() {
            log.info("securityMethod1 worked");
        }

        @Auth("admin/credentials")
        public void securityMethod2() {
            log.info("securityMethod2 worked");
        }
    }
}
