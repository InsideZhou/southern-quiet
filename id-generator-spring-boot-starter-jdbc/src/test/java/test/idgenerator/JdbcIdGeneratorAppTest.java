package test.idgenerator;

import com.ai.southernquiet.util.JdbcIdGeneratorAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ImportAutoConfiguration(JdbcIdGeneratorAutoConfiguration.class)
public class JdbcIdGeneratorAppTest {
    public static void main(String[] args) {
        SpringApplication.run(JdbcIdGeneratorAppTest.class);
    }
}
