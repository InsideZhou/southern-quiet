package test.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@org.springframework.boot.test.autoconfigure.json.JsonTest
@RunWith(SpringRunner.class)
public class JsonTest {
    @SpringBootConfiguration
    public static class Config {}

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
            Account other = (Account) mapper.readValue(json, Object.class);
            System.out.println(other);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
