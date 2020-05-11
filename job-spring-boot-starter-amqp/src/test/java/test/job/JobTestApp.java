package test.job;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.job.AmqpJobAutoConfiguration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ImportAutoConfiguration({FrameworkAutoConfiguration.class, AmqpJobAutoConfiguration.class})
public class JobTestApp {
    public static void main(String[] args) {
        SpringApplication.run(JobTestApp.class, args);
    }

    @ConditionalOnMissingBean
    @Bean
    public static RabbitTransactionManager rabbitTransactionManager(ConnectionFactory connectionFactory) {
        RabbitTransactionManager manager = new RabbitTransactionManager();
        manager.setConnectionFactory(connectionFactory);
        return manager;
    }

    @Bean
    public JobTest.Listener listener() {
        return new JobTest.Listener();
    }
}
