package test.filesystem;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.filesystem.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest(classes = FrameworkAutoConfiguration.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FileSystemTest {
    @Autowired
    private FileSystem fileSystem;

    @BeforeAll
    public void before() {}

    @Test
    public void arrayCopyOf() {
        Assertions.assertArrayEquals(new String[0], Arrays.copyOf(new String[]{"name"}, 0));
    }

    @Test
    public void splitString() {
        Assertions.assertArrayEquals(new String[]{"name"}, "name".split("/"));
        Assertions.assertArrayEquals(new String[]{}, "/".split("/"));
        Assertions.assertArrayEquals(new String[]{""}, "".split("/"));
    }

    @Test
    public void joinString() {
        Assertions.assertEquals("name", String.join("/", "name"));
        Assertions.assertEquals("/", String.join("/", "/"));
        Assertions.assertEquals("//", String.join("/", "/", ""));
        Assertions.assertEquals("//name", String.join("/", "/", "name"));
    }

    @Test
    public void path() {
        NormalizedPath normalizedPath = new NormalizedPath("hello.text");
        Assertions.assertEquals("/hello.text", normalizedPath.toString());

        normalizedPath = new NormalizedPath("");
        Assertions.assertEquals("/", normalizedPath.toString());

        normalizedPath = new NormalizedPath("/");
        Assertions.assertEquals("/", normalizedPath.toString());

        normalizedPath = new NormalizedPath("//test////hello.text/");
        Assertions.assertEquals("/test/hello.text", normalizedPath.toString());

        Assertions.assertEquals(0, "/".split("/").length);
        Assertions.assertEquals(2, "/abc".split("/").length);
        Assertions.assertEquals(1, "abc/".split("/").length);

        normalizedPath = new NormalizedPath("hello.text");
        Assertions.assertEquals("/", normalizedPath.getParent());
        Assertions.assertEquals("hello.text", normalizedPath.getName());

        normalizedPath = new NormalizedPath("/");
        Assertions.assertEquals("", normalizedPath.getParent());
        Assertions.assertEquals("/", normalizedPath.getName());

        normalizedPath = new NormalizedPath("/hello.text");
        Assertions.assertEquals("/", normalizedPath.getParent());
        Assertions.assertEquals("hello.text", normalizedPath.getName());

        normalizedPath = new NormalizedPath("/test/hello.text");
        Assertions.assertEquals("/test", normalizedPath.getParent());
        Assertions.assertEquals("hello.text", normalizedPath.getName());
    }

    @Test
    public void simpleIO() {
        try {
            fileSystem.put("hello/world.txt", "你好，Spring Boot。");
            Assertions.assertEquals("你好，Spring Boot。", fileSystem.read("hello/world.txt"));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }

        fileSystem.delete("hello/world.txt");
        Assertions.assertFalse(fileSystem.exists("hello/world.txt"));
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

        Assertions.assertEquals("你好，Spring Boot。", result);
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

        Assertions.assertTrue(fileSystem.exists(file));
        try {
            Assertions.assertTrue(fileSystem.files("/", file).anyMatch(meta -> meta.getName().equals(file)));
        }
        catch (PathNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void files() {
        try {
            fileSystem.put("hello/world.txt", "你好，Spring Boot。");
            fileSystem.put("hello/girl/lily.txt", "Hello，美女。");

            List<? extends PathMeta> files = fileSystem.files("hello", true).collect(Collectors.toList());

            Assertions.assertEquals(2, files.size());

            Assertions.assertEquals(1, files.stream().filter(f -> f.getPath().equals("/hello/world.txt")).count());
            Assertions.assertEquals(1, files.stream().filter(f -> f.getPath().equals("/hello/girl/lily.txt")).count());
        }
        catch (InvalidFileException | PathNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void createSymbolicLink() {
        try {
            String link = "hello/link";
            String target = "hello/world.txt";
            fileSystem.put(target, "你好，Spring Boot。");
            fileSystem.createSymbolicLink(link, target);

            try (InputStream inputStream1 = fileSystem.openReadStream(link);
                 InputStream inputStream2 = fileSystem.openReadStream(target)) {
                //1.读取软链接和源文件,指纹一样
                String hash1 = DigestUtils.md5DigestAsHex(inputStream1);
                String hash2 = DigestUtils.md5DigestAsHex(inputStream2);

                Assertions.assertEquals(hash1, hash2);
            }
            catch (InvalidFileException | IOException e) {
                throw new RuntimeException(e);
            }

            //2.删掉软链接,源文件不受影响
            fileSystem.delete(link);
            Assertions.assertTrue(fileSystem.exists(target));

            //3.删掉源文件,软链接失效
            fileSystem.delete(target);
            Assertions.assertFalse(fileSystem.exists(link));
        }
        catch (InvalidFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void writeSymbolicLink() throws InvalidFileException, IOException {
        String link = "hello/symbolicLink";
        String target = "hello/target.txt";
        fileSystem.put(target, "");
        fileSystem.createSymbolicLink(link, target);

        String content = "write symbolicLink";

        try (OutputStream outputStream = fileSystem.openWriteStream(link)) {
            outputStream.write(content.getBytes());
            outputStream.flush();
        }
        catch (InvalidFileException e) {
            e.printStackTrace();
        }

        try (InputStream inputStream = fileSystem.openReadStream(target)) {
            byte[] bytes = StreamUtils.copyToByteArray(inputStream);
            String result = new String(bytes);

            //验证通过软链接 可以直接编辑源文件
            Assertions.assertEquals(content, result);
        }
        catch (InvalidFileException e) {
            e.printStackTrace();
        }

        fileSystem.delete(link);
        fileSystem.delete(target);
    }

}
