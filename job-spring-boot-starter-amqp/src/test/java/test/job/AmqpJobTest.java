package test.job;

import me.insidezhou.southernquiet.job.JobEngine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AmqpJobTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class Config {
        @ConditionalOnMissingBean
        @Bean
        public RabbitTransactionManager rabbitTransactionManager(ConnectionFactory connectionFactory) {
            RabbitTransactionManager manager = new RabbitTransactionManager();
            manager.setConnectionFactory(connectionFactory);
            return manager;
        }
    }

    @Autowired
    private JobEngine jobEngine;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @SuppressWarnings("unchecked")
    @Test
    public void prepare() {
        jobEngine.arrange(new AmqpJob());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = Exception.class)
    public void prepareButException() {
        transactionTemplate.execute(status -> {
            jobEngine.arrange(new AmqpOtherJob());
            throw new RuntimeException("引擎编排任务的事务中抛出异常");
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void prepareException() {
        transactionTemplate.execute(status -> {
            jobEngine.arrange(new AmqpExceptionJob());
            return "";
        });
    }
}
