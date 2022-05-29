package test.throttle;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.throttle.annotation.ThrottledSchedule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class})
public class ThrottleTestApp {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(ThrottleTestApp.class);

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
        private String cron = "*/5 * * * * *";

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class ScheduledThrottleBean {
        private final long begin = System.currentTimeMillis();

        @ThrottledSchedule(cron = "#{properties.cron}", name = "scheduledThrottleMethod1")
        public void scheduledThrottleMethod1() {
            log.message("scheduledThrottleMethod1 working ...")
                .context("begin", begin)
                .context("elapsed", Duration.ofMillis(System.currentTimeMillis() - begin))
                .info();
        }

//        @ThrottledSchedule(fixedRate = 1000, name = "scheduledThrottleMethod2")
        public void scheduledThrottleMethod2() {
            log.message("scheduledThrottleMethod2 working ...")
                .context("begin", begin)
                .context("elapsed", Duration.ofMillis(System.currentTimeMillis() - begin))
                .info();
        }

//        @ThrottledSchedule(fixedRate = 1000, name = "scheduledThrottleMethod3")
        public void scheduledThrottleMethod3() {
            log.message("scheduledThrottleMethod3 working ...")
                .context("begin", begin)
                .context("elapsed", Duration.ofMillis(System.currentTimeMillis() - begin))
                .info();
        }
    }
}
