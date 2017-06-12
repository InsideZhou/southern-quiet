package test.filesystem;

import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class MongoDbFileSystemTest extends FileSystemTest {
    @Configuration
    @EnableAutoConfiguration
    @ComponentScan({"com.ai.southernquiet.filesystem"})
    public static class Config {}
}
