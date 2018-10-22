package test.job;

import com.ai.southernquiet.job.JobEngine;
import com.ai.southernquiet.job.JobProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AmqpJobTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class Config {
        @Bean
        public JobProcessor<AmqpJob> jobProcessor() {
            return new JobProcessor<AmqpJob>() {
                @Override
                public void process(AmqpJob job) {
                    System.out.println(job.getId());
                }

                @Override
                public Class<AmqpJob> getJobClass() {
                    return AmqpJob.class;
                }
            };
        }

        @Bean
        public JobProcessor<AmqpExceptionJob> exceptionJobJobProcessor() {
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

    @Autowired
    private JobEngine jobEngine;

    @SuppressWarnings("unchecked")
    @Test
    public void prepare() {
        jobEngine.arrange(new AmqpJob());
    }

//    @SuppressWarnings("unchecked")
//    @Test(expected = Exception.class)
//    public void prepareButException() {
//        jobEngine.arrange(new AmqpExceptionJob());
//    }
}
