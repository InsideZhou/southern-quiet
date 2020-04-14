package test.throttle;

import me.insidezhou.southernquiet.throttle.lua.LocalResourceUtil;
import net.bytebuddy.utility.RandomString;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.StringUtils;

public class LocalResourceUtilTest {

    @Test
    public void getSource() {
        String source = StringUtils.trimAllWhitespace(LocalResourceUtil.getSource("/lua/test.lua"));
        Assert.assertEquals("return",source );

        Assert.assertNull(LocalResourceUtil.getSource(RandomString.make()));
    }

}
