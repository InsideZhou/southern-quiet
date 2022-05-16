package test.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@org.springframework.boot.test.autoconfigure.json.JsonTest
@ExtendWith(SpringExtension.class)
public class JsonTest {
    private static final SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(JsonTest.class);

    @Configuration
    @EnableAutoConfiguration
    public static class Config {
        @Bean
        @Primary
        static ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private ObjectMapper mapper;

    public static class Account {
        private String name;
        private Set<Role> roles = new HashSet<>();

        public Set<Role> getRoles() {
            return roles;
        }

        public void setRoles(Set<Role> roles) {
            this.roles = roles;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Role {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void jsonTest() {
        Account account = new Account();
        account.setName("superman");
        Role role = new Role();
        role.setId(-1);
        role.setName("admin");
        account.getRoles().add(role);

        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

        try {
            String json = mapper.writeValueAsString(account);
            Account other = mapper.readValue(json, Account.class);
            Role otherRole = (Role) other.getRoles().toArray()[0];
            Assertions.assertEquals(role.getId(), otherRole.getId());
            Assertions.assertEquals(role.getName(), otherRole.getName());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
