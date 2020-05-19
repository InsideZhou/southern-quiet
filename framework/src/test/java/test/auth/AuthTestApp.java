package test.auth;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.auth.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class})
public class AuthTestApp {
    private final static Logger log = LoggerFactory.getLogger(AuthTestApp.class);

    public static void main(String[] args) {
        SpringApplication.run(AuthTestApp.class, args);
    }

    @RestController
    public static class Controller implements SecurityService {
        @Auth
        @RequestMapping("action1")
        @Override
        public void action() {
            log.debug("action working");
        }

        @RequestMapping("action2")
        public void action2() {
            log.debug("action2 working");
        }
    }
}
