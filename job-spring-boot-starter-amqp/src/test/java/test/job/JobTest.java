package test.job;

import me.insidezhou.southernquiet.job.JobArranger;
import me.insidezhou.southernquiet.job.JobProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JobTest {
    private final static Logger log = LoggerFactory.getLogger(JobTest.class);

    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class Config {}

    @Autowired
    private JobArranger<Serializable> jobArranger;

    @Test
    public void dummy() {
        jobArranger.arrange(new AmqpJob());
    }

    @Transactional()
    @Test(expected = RuntimeException.class)
    public void tx() {
        jobArranger.arrange(new AmqpJob());
        throw new RuntimeException("tx");
    }

    public static class Listener {
        @JobProcessor(job = AmqpJob.class, name = "a")
        @JobProcessor(job = AmqpJob.class, name = "b")
        @JobProcessor(job = AmqpJob.class, name = "e")
        public void standard(AmqpJob job, JobProcessor processor) {
            log.info("使用监听器{}接到任务：{}", processor.name(), job.getId());
        }

        @JobProcessor(job = AmqpJob.class, name = "e")
        public void exception(AmqpJob job, JobProcessor processor) {
            throw new RuntimeException("在任务中抛出异常通知：listener=" + processor.name() + ", job=" + job.getId());
        }
    }
}
