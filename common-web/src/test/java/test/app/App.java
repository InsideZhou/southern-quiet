package test.app;

import com.ai.southernquiet.broadcasting.BroadcastingDone;
import com.ai.southernquiet.broadcasting.Publisher;
import com.ai.southernquiet.util.BCrypt;
import com.ai.southernquiet.util.IdGenerator;
import com.ai.southernquiet.web.AbstractWebApp;
import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import com.ai.southernquiet.web.auth.*;
import instep.InstepLogger;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

@SuppressWarnings("unused")
@RestController
@SpringBootApplication
public class App extends AbstractWebApp {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        SpringApplication.run(App.class);
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Configuration
    public static class Config {
        @SuppressWarnings("Duplicates")
        @Bean
        public AuthService authService(CommonWebAutoConfiguration.WebProperties webProperties) {
            return new AuthService() {
                private Logger logger = LoggerFactory.getLogger(App.class);

                private User<Account> user = new User<>(
                    () -> "superman",
                    "2636d11c-7e52-4d12-80b5-893116c20cce",
                    webProperties.getAuthenticationTTL()
                );

                @Override
                public User<Account> authenticate(String username, String password, boolean remember) throws AuthException {
                    user.setAuthenticationTime(Instant.now());

                    if (!user.getAccount().getName().equals(username)) throw new UserNotFoundException(username);
                    String hashed = BCrypt.hashpw("givemefive", BCrypt.gensalt());
                    logger.debug(hashed);
                    if (!BCrypt.checkpw(password, hashed)) {
                        throw new IncorrectPasswordException("");
                    }

                    return user;
                }

                @Override
                public <R extends Request> User<?> authenticate(R request) {
                    return null;
                }

                @Override
                public User<Account> getUserByRememberToken(String token) {
                    if (!token.equals(user.getRememberToken())) return null;

                    return user;
                }

                @Override
                public boolean checkAuthorization(String username, Set<String> authNames) {
                    return true;
                }
            };
        }

        @Bean
        public InstepLogger instepLogger() {
            return new InstepLogger() {
                @Override
                public boolean getEnableDebug() {
                    return true;
                }

                @Override
                public boolean getEnableInfo() {
                    return true;
                }

                @Override
                public boolean getEnableWarning() {
                    return true;
                }

                @Override
                public void debug(String s, String s1) {
                    LogFactory.getLog(s1).debug(s);
                }

                @Override
                public void info(String s, String s1) {
                    LogFactory.getLog(s1).info(s);
                }

                @Override
                public void warning(String s, String s1) {
                    LogFactory.getLog(s1).warn(s);
                }

                @Override
                public void debug(String s, Class<?> aClass) {
                    debug(s, aClass.getName());
                }

                @Override
                public void info(String s, Class<?> aClass) {
                    info(s, aClass.getName());
                }

                @Override
                public void warning(String s, Class<?> aClass) {
                    warning(s, aClass.getName());
                }
            };
        }
    }

    @RequestMapping("/")
    String home() {
        logger.debug("你好，Spring Boot！");
        return "Hello World!";
    }

    @PostMapping("/login")
    String login(Request request, String username, String password) {
        try {
            request.login(username, password, true);
        }
        catch (UserNotFoundException e) {
            return "UserNotFound " + e.getMessage();
        }
        catch (IncorrectPasswordException e) {
            return "IncorrectPassword " + e.getMessage();
        }
        catch (AuthException e) {
            return e.getMessage();
        }

        return "DONE";
    }

    @Auth
    @RequestMapping("/user")
    String user(HttpServletRequest request) {
        return request.getRemoteUser();
    }

    @Autowired
    private Publisher<Serializable> publisher;

    @RequestMapping("/broadcast")
    void broadcast() {
        publisher.publish(new BroadcastingDone());
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private IdGenerator idGenerator;

    @RequestMapping("/generateId")
    long generateId() throws Exception {
        return idGenerator.generate();
    }
}
