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
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@SpringBootTest(classes = FrameworkAutoConfiguration.class)
@RunWith(SpringRunner.class)
public class FileSystemTest {
    @Autowired
    private FileSystem fileSystem;

    @Before
    public void before() {}

    @Test
    public void path() {
        String normalizedPath = FileSystem.normalizePath("hello.text");
        Assert.assertEquals("/hello.text", normalizedPath);

        normalizedPath = FileSystem.normalizePath("");
        Assert.assertEquals("/", normalizedPath);

        normalizedPath = FileSystem.normalizePath("/");
        Assert.assertEquals("/", normalizedPath);

        normalizedPath = FileSystem.normalizePath("//test////hello.text/");
        Assert.assertEquals(normalizedPath, "/test/hello.text");

        Assert.assertTrue("/".split("/").length == 0);
        Assert.assertTrue("/abc".split("/").length == 2);
        Assert.assertTrue("abc/".split("/").length == 1);

        normalizedPath = FileSystem.normalizePath("hello.text");
        Assert.assertEquals("/", FileSystem.getPathParent(normalizedPath));
        Assert.assertEquals("hello.text", FileSystem.getPathName(normalizedPath));

        normalizedPath = FileSystem.normalizePath("/");
        Assert.assertEquals("", FileSystem.getPathParent(normalizedPath));
        Assert.assertEquals("/", FileSystem.getPathName(normalizedPath));

        normalizedPath = FileSystem.normalizePath("/hello.text");
        Assert.assertEquals("/", FileSystem.getPathParent(normalizedPath));
        Assert.assertEquals("hello.text", FileSystem.getPathName(normalizedPath));

        normalizedPath = FileSystem.normalizePath("/test/hello.text");
        Assert.assertEquals("/test", FileSystem.getPathParent(normalizedPath));
        Assert.assertEquals("hello.text", FileSystem.getPathName(normalizedPath));
    }

    @Test
    public void simpleIO() {
        try {
            fileSystem.put("hello.txt", "你好，Spring Boot。");
            Assert.assertEquals("你好，Spring Boot。", fileSystem.read("hello.txt"));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }

        fileSystem.delete("hello.txt");
        Assert.assertFalse(fileSystem.exists("hello.txt"));
    }

    @Test
    public void streamingIO() {
        String path = "streaming/hello/world.txt";

        fileSystem.delete(path);

        try (OutputStream outputStream = fileSystem.openWriteStream(path)) {
            outputStream.write("你好，".getBytes(StandardCharsets.UTF_8));
        }
        catch (InvalidFileException | IOException e) {
            throw new RuntimeException(e);
        }

        try (OutputStream outputStream = fileSystem.openWriteStream(path)) {
            outputStream.write("Spring Boot。".getBytes(StandardCharsets.UTF_8));
        }
        catch (InvalidFileException | IOException e) {
            throw new RuntimeException(e);
        }

        String result;
        try (InputStream inputStream = fileSystem.openReadStream(path)) {
            result = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
        catch (InvalidFileException | IOException e) {
            throw new RuntimeException(e);
        }

        Assert.assertEquals("你好，Spring Boot。", result);
    }

    @Test
    public void writeAndFind() {
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
