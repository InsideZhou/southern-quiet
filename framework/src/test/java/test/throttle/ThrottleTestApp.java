package test.throttle;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.throttle.annotation.ThrottledSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class})
public class ThrottleTestApp {
    private final static Logger log = LoggerFactory.getLogger(ThrottleTestApp.class);

    public static void main(String[] args) {
        SpringApplication.run(ThrottleTestApp.class, args);
    }

    @Bean
    public ScheduledThrottleBean scheduledThrottleBean() {
        return new ScheduledThrottleBean();
    }

    @Bean
    @ConfigurationProperties("test.throttle")
    public Properties properties() {
        return new Properties();
    }

    public static class Properties {
        private String cron = "0 * * * * *";

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class ScheduledThrottleBean {
        private final long begin = System.currentTimeMillis();

        @ThrottledSchedule(cron = "#{properties.cron}", name = "scheduledThrottleMethod")
        public void scheduledThrottleMethod1() {
            log.info("scheduledThrottleMethod1 working ...\tbegin={}, elapsed={}", begin, Duration.ofMillis(System.currentTimeMillis() - begin));
        }

//        @ThrottledSchedule(fixedRate = 1000, name = "scheduledThrottleMethod")
        public void scheduledThrottleMethod2() {
            log.info("scheduledThrottleMethod2 working ...\tbegin={}, elapsed={}", begin, Duration.ofMillis(System.currentTimeMillis() - begin));
        }

//        @ThrottledSchedule(fixedRate = 1000, name = "scheduledThrottleMethod")
        public void scheduledThrottleMethod3() {
            log.info("scheduledThrottleMethod3 working ...\tbegin={}, elapsed={}", begin, Duration.ofMillis(System.currentTimeMillis() - begin));
        }
    }
}
