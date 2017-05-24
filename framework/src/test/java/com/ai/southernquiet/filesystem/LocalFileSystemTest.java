package com.ai.southernquiet.filesystem;

import com.ai.southernquiet.filesystem.driver.LocalFileSystem;
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
    @EnableConfigurationProperties
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
}
