package test.idgenerator;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.util.IdGenerator;
import me.insidezhou.southernquiet.idgenerator.JdbcIdGeneratorAutoConfiguration;
import me.insidezhou.southernquiet.util.SnowflakeIdGenerator;
import instep.springboot.CoreAutoConfiguration;
import instep.springboot.SQLAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JdbcIdGeneratorTest {
    @SpringBootConfiguration
    @ImportAutoConfiguration({
        JdbcIdGeneratorAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        CoreAutoConfiguration.class,
        SQLAutoConfiguration.class,
        FrameworkAutoConfiguration.class
    })
    public static class Config {}

    @Autowired
    private IdGenerator idGenerator;

    @Test
    public void generate() {
        long counter = 0;

        long ts = System.currentTimeMillis();
        while (System.currentTimeMillis() - ts < 1000) {
            counter += 1;
            idGenerator.generate();
        }

        System.out.println(counter);
    }

    @Test
    public void snowflakeGenerate() {
        long counter = 0;
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(0);

        long ts = System.currentTimeMillis();
        while (System.currentTimeMillis() - ts < 1000) {
            counter += 1;
            idGenerator.generate();
        }

        System.out.println(counter);
    }
}
