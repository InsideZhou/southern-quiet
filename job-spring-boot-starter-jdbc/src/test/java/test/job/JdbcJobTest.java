package test.job;

import com.ai.southernquiet.job.JdbcJobAutoConfiguration;
import com.ai.southernquiet.job.JobHandler;
import com.ai.southernquiet.job.JobQueue;
import instep.dao.sql.Dialect;
import instep.springboot.CoreAutoConfiguration;
import instep.springboot.SQLAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.Serializable;

@RunWith(SpringRunner.class)
@ImportAutoConfiguration({DataSourceAutoConfiguration.class, CoreAutoConfiguration.class, SQLAutoConfiguration.class})
@SpringBootTest(classes = {JdbcJobAutoConfiguration.class, JdbcJobTest.Config.class})
public class JdbcJobTest {
    @Configuration
    public static class Config {
        @Bean
        public JobHandler<JdbcJob> handler() {
            return new JobHandler<JdbcJob>() {
                @Override
                public void handle(JdbcJob job) {
                    System.out.println(job.getId() + "@" + getJobClass().getName());
                }

                @Override
                public Class<JdbcJob> getJobClass() {
                    return JdbcJob.class;
                }
            };
        }

        @Bean
        public JobHandler<JobException> exceptionJobHandler() {
            return new JobHandler<JobException>() {
                @Override
                public void handle(JobException job) throws Exception {
                    throw job;
                }

                @Override
                public Class<JobException> getJobClass() {
                    return JobException.class;
                }
            };
        }

        @Bean
        public Dialect dialect(@Value("${spring.datasource.url}") String url) {
            return Dialect.Companion.of(url);
        }
    }

    @Autowired
    private JobQueue<Serializable> jobQueue;

    @Test
    public void enqueue() {
        jobQueue.enqueue(new JdbcJob());
        jobQueue.enqueue(new JobException());
    }
}
