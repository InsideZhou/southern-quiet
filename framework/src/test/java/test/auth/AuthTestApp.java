package test.auth;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.auth.Auth;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.*;

@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class})
public class AuthTestApp {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(AuthTestApp.class);

    public static void main(String[] args) {
        SpringApplication.run(AuthTestApp.class, args);
    }

    @RestController
    public static class Controller implements SecurityService {
        @RequestAuthMapping("action1")
        @Override
        public void action() {
            log.message("action working").debug();
        }

        @RequestMapping("action2")
        public void action2() {
            log.message("action2 working").debug();
        }
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Documented
    @RequestMapping
    @Auth
    public @interface RequestAuthMapping {
        @AliasFor(annotation = RequestMapping.class)
        String name() default "";

        @AliasFor(annotation = RequestMapping.class)
        String[] value() default {};

        @AliasFor(annotation = RequestMapping.class)
        String[] path() default {};

        @AliasFor(annotation = RequestMapping.class)
        RequestMethod[] method() default {};

        @AliasFor(annotation = RequestMapping.class)
        String[] params() default {};

        @AliasFor(annotation = RequestMapping.class)
        String[] headers() default {};

        @AliasFor(annotation = RequestMapping.class)
        String[] consumes() default {};

        @AliasFor(annotation = RequestMapping.class)
        String[] produces() default {};
    }
}
