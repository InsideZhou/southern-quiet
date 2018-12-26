package test;

import com.ai.southernquiet.file.web.FileWebFluxAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ImportAutoConfiguration(FileWebFluxAutoConfiguration.class)
public class FileWebTest {
    public static void main(String[] args) {
        SpringApplication.run(FileWebTest.class);
    }
}
