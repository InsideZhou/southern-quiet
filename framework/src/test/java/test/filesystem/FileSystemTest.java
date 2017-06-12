package test.filesystem;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.InvalidFileException;
import com.ai.southernquiet.filesystem.PathNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = FrameworkAutoConfiguration.class)
@RunWith(SpringRunner.class)
public class FileSystemTest {
    @Autowired
    private FileSystem fileSystem;

    @Before
    public void before() {
        System.out.println(fileSystem);
    }

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

    @Test
    public void fileExistsTest() {
        String file = "exists.txt";
        try {
            fileSystem.put(file, "你好，Spring Boot。");
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }

        Assert.assertTrue(fileSystem.exists(file));
        try {
            Assert.assertTrue(fileSystem.files("/", file).anyMatch(meta -> meta.getName().equals(file)));
        }
        catch (PathNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
