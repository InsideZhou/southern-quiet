package test.filesystem;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MongoDbFileSystemTest extends FileSystemTest {
    @Configuration
    @EnableAutoConfiguration
    @ComponentScan({"me.insidezhou.southernquiet.filesystem"})
    public static class Config {}
}
