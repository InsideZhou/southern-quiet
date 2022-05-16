package test.throttle;

import me.insidezhou.southernquiet.throttle.lua.LocalResourceUtil;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

public class LocalResourceUtilTest {

    @Test
    public void getSource() {
        String source = StringUtils.trimAllWhitespace(LocalResourceUtil.getSource("/lua/test.lua"));
        Assertions.assertEquals("return", source);
        Assertions.assertNull(LocalResourceUtil.getSource(RandomString.make()));
    }
}
