package test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ExpirationTest {
    @Test
    public void pow() {
        long expiration = 5000;

        int i = 0;
        while (i < 10) {
            expiration += Math.pow(expiration, 1.1);
            System.out.println(expiration);
            i++;
        }
    }
}
