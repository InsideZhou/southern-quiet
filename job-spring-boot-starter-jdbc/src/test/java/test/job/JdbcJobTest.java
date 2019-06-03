package test.job;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.job.FailedJobTable;
import me.insidezhou.southernquiet.job.JdbcJobAutoConfiguration;
import me.insidezhou.southernquiet.job.JobProcessor;
import me.insidezhou.southernquiet.job.JobEngine;
import me.insidezhou.southernquiet.job.driver.JdbcJobEngine;
import me.insidezhou.southernquiet.job.driver.ProcessorNotFoundException;
import instep.dao.sql.InstepSQL;
import instep.springboot.CoreAutoConfiguration;
import instep.springboot.SQLAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ImportAutoConfiguration({FrameworkAutoConfiguration.class, DataSourceAutoConfiguration.class, CoreAutoConfiguration.class, SQLAutoConfiguration.class})
@SpringBootTest(classes = {JdbcJobAutoConfiguration.class, JdbcJobTest.Config.class})
public class JdbcJobTest {
    @Configuration
    public static class Config {
        @Bean
        public JobProcessor<JdbcJob> handler() {
            return new JobProcessor<JdbcJob>() {
                @Override
                public void process(JdbcJob job) {
                    System.out.println(String.format("Job(%s) processing on Thread(%s)", job.getId(), Thread.currentThread().getId()));
                }

                @Override
                public Class<JdbcJob> getJobClass() {
                    return JdbcJob.class;
                }
            };
        }

        @Bean
        public JobProcessor<JobException> exceptionJobHandler() {
            return new JobProcessor<JobException>() {
                @Override
                public void process(JobException job) throws Exception {
                    throw job;
                }

                @Override
                public Class<JobException> getJobClass() {
                    return JobException.class;
                }
            };
        }

        @Bean
        public JobProcessor<NonSerializableJob> nonSerializableJobJobProcessor() {
            return new JobProcessor<NonSerializableJob>() {
                @Override
                public void process(NonSerializableJob job) throws Exception {
                    throw new Exception(job.getClass().getName());
                }

                @Override
                public Class<NonSerializableJob> getJobClass() {
                    return NonSerializableJob.class;
                }
            };
        }
    }

    @Autowired
    private JobEngine jobEngine;

    @Autowired
    private FailedJobTable failedJobTable;

    @Autowired
    private InstepSQL instepSQL;

    @SuppressWarnings("unchecked")
    @Test
    public void enqueue() {
        JdbcJob job = new JdbcJob();
        System.out.println(String.format("Job(%s) arrange on Thread(%s)", job.getId(), Thread.currentThread().getId()));
        jobEngine.arrange(job);
        jobEngine.arrange(new JobException());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ClassCastException.class)
    public void enqueueWithNonSerializable() {
        jobEngine.arrange(new NonSerializableJob());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ProcessorNotFoundException.class)
    public void enqueueWithException() {
        jobEngine.arrange(new NoProcessorJob());
    }

    @Test
    public void queryFailedJob() {
        JdbcJobEngine jdbcJobQueue = (JdbcJobEngine) jobEngine;

        jdbcJobQueue.retryFailedJob();
    }

    @Test
    public void queryDirtyStatus() {
        JdbcJobEngine jdbcJobQueue = (JdbcJobEngine) jobEngine;
        jdbcJobQueue.cleanWorkingStatus();
    }
}
