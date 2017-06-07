package test.filesystem;

import com.ai.southernquiet.filesystem.InvalidFileException;
import com.ai.southernquiet.filesystem.driver.MongoDbFileSystem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class MongoDbFileSystemTest {
    @Configuration
    @EnableAutoConfiguration
    @ComponentScan({"com.ai.southernquiet.filesystem"})
    public static class Config {}

    @Autowired
    private MongoDbFileSystem fileSystem;

    @Test
    public void simpleFileReadWriteTest() {
        try {
            fileSystem.put("hello.txt", "你好，Spring Boot。");
            Assert.assertEquals(fileSystem.read("hello.txt"), "你好，Spring Boot。");
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }
}
