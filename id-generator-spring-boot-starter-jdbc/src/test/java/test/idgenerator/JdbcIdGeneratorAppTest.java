package test.idgenerator;

import me.insidezhou.southernquiet.idgenerator.JdbcIdGeneratorAutoConfiguration;
import me.insidezhou.southernquiet.instep.InstepAutoConfiguration;
import me.insidezhou.southernquiet.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@ImportAutoConfiguration({JdbcIdGeneratorAutoConfiguration.class, InstepAutoConfiguration.class})
@RestController
public class JdbcIdGeneratorAppTest {
    public static void main(String[] args) {
        SpringApplication.run(JdbcIdGeneratorAppTest.class, args);
    }

    @Autowired
    private IdGenerator idGenerator;

    @RequestMapping("getId")
    public long getId() {
        return idGenerator.generate();
    }
}
