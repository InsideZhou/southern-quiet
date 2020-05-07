package test.auth;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.auth.Auth;
import me.insidezhou.southernquiet.auth.NoAuthProviderExistsException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.UndeclaredThrowableException;

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

    @Test
    public void auth() {
        NoAuthProviderExistsException noAuthProviderExistsException = null;

        try {
            securityTarget.securityMethod1();
        }
        catch (UndeclaredThrowableException e) {
            noAuthProviderExistsException = (NoAuthProviderExistsException) e.getCause();
        }

        Assert.assertNotNull(noAuthProviderExistsException);
    }

    public static class SecurityTarget {
        @Auth
        public void securityMethod1() {
            log.info("securityMethod1 worked");
        }
    }
}
