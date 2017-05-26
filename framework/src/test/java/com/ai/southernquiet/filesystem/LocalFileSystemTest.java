package com.ai.southernquiet.filesystem;

import com.ai.southernquiet.FrameworkProperties;
import com.ai.southernquiet.filesystem.driver.LocalFileSystem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class LocalFileSystemTest {
    @Configuration
    @EnableConfigurationProperties(FrameworkProperties.class)
    @ComponentScan({"com.ai.southernquiet.filesystem"})
    public static class Config {}

    @Autowired
    private LocalFileSystem fileSystem;

    @Test
    public void createFileTest() {
        try {
            fileSystem.put("hello.txt", "你好，Spring Boot。");
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
            Assert.assertTrue(fileSystem.files("/", file).stream().anyMatch(meta -> meta.getName().equals(file)));
        }
        catch (PathNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
