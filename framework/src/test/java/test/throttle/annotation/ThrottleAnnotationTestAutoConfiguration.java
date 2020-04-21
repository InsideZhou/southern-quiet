package test.throttle.annotation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThrottleAnnotationTestAutoConfiguration {

    @Bean
    public ThrottleAnnotationTestProcessor throttleAnnotationTestProcessor() {
        return new ThrottleAnnotationTestProcessor();
    }

}
