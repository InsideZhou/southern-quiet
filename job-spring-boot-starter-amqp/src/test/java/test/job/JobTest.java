package test.job;

import me.insidezhou.southernquiet.job.JobArranger;
import me.insidezhou.southernquiet.job.JobProcessor;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class JobTest {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(JobTest.class);

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
    @Test
    public void tx() {
        jobArranger.arrange(new AmqpJob());
        Assertions.assertThrows(RuntimeException.class, () -> {
            throw new RuntimeException("tx");
        });
    }

    public static class Listener {
        @JobProcessor(job = AmqpJob.class, name = "a")
        @JobProcessor(job = AmqpJob.class, name = "b")
        @JobProcessor(job = AmqpJob.class, name = "e")
        public void standard(AmqpJob job, JobProcessor processor) {
            log.message("使用监听器接到任务")
                .context("listener", processor.name())
                .context("job", job.getId())
                .info();
        }

        @JobProcessor(job = AmqpJob.class, name = "e")
        public void exception(AmqpJob job, JobProcessor processor) {
            throw new RuntimeException("在任务中抛出异常：listener=" + processor.name() + ", job=" + job.getId());
        }
    }
}
