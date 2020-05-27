package test.auth;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.auth.Auth;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class})
public class AuthTestApp {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AuthTestApp.class);

    public static void main(String[] args) {
        SpringApplication.run(AuthTestApp.class, args);
    }

    @RestController
    public static class Controller implements SecurityService {
        @Auth
        @RequestMapping("action1")
        @Override
        public void action() {
            log.message("action working").debug();
        }

        @RequestMapping("action2")
        public void action2() {
            log.message("action2 working").debug();
        }
    }
}
