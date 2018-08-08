package test;

import com.ai.southernquiet.file.http.FileHttpServiceAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ImportAutoConfiguration(FileHttpServiceAutoConfiguration.class)
public class FileHttpServiceTest {
    public static void main(String[] args) {
        SpringApplication.run(FileHttpServiceTest.class);
    }
}
