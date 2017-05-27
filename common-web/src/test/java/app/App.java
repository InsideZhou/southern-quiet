package app;

import com.ai.southernquiet.util.BCrypt;
import com.ai.southernquiet.web.auth.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

@RestController
@SpringBootApplication(scanBasePackages = {"com.ai.southernquiet", "app"})
public class App implements WebMvcConfigurer {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(App.class);
    }

    @Bean
    static AuthService authService() {
        Logger logger = LoggerFactory.getLogger(App.class);

        return new AuthService() {
            private User user = new User("superman", "2636d11c-7e52-4d12-80b5-893116c20cce");

            @Override
            public User authenticate(String username, String password, boolean remember) throws AuthException {
                user.setAuthenticationTime(System.currentTimeMillis());

                if (!user.getUsername().equals(username)) throw new UserNotFoundException(username);
                String hashed = BCrypt.hashpw("givemefive", BCrypt.gensalt());
                logger.debug(hashed);
                if (!BCrypt.checkpw(password, hashed)) {
                    throw new IncorrectPasswordException("");
                }

                return user;
            }

            @Override
            public User getUserByRememberToken(String token) {
                if (!token.equals(user.getRememberToken())) return null;

                return user;
            }

            @Override
            public boolean checkAuthorization(String username, Set<String> authNames) {
                return true;
            }
        };
    }

    @Autowired
    private AuthService authService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(authService));
    }

    private Logger logger = LoggerFactory.getLogger(App.class);

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
}
