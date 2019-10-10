package test.job;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.job.AmqpJobAutoConfiguration;
import me.insidezhou.southernquiet.job.JobProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class, AmqpJobAutoConfiguration.class})
public class AmqpJobTestApp {
    private static Logger log = LoggerFactory.getLogger(AmqpJobTestApp.class);

    public static void main(String[] args) {
        SpringApplication.run(AmqpJobTestApp.class, args);
    }

    @Bean
    @ConditionalOnMissingBean
    public static RabbitTransactionManager rabbitTransactionManager(ConnectionFactory connectionFactory) {
        RabbitTransactionManager manager = new RabbitTransactionManager();
        manager.setConnectionFactory(connectionFactory);
        return manager;
    }

    @Bean
    public static JobProcessor<AmqpJob> jobProcessor() {
        return new JobProcessor<AmqpJob>() {
            @Override
            public void process(AmqpJob job) {
                log.debug("{}", job.getId());
            }

            @Override
            public Class<AmqpJob> getJobClass() {
                return AmqpJob.class;
            }
        };
    }

    @Bean
    public static JobProcessor<AmqpExceptionJob> exceptionJobJobProcessor() {
        return new JobProcessor<AmqpExceptionJob>() {
            @Override
            public void process(AmqpExceptionJob job) throws Exception {
                throw new Exception(job.getId().toString());
            }

            @Override
            public Class<AmqpExceptionJob> getJobClass() {
                return AmqpExceptionJob.class;
            }
        };
    }
}
