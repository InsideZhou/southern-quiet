package test;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.util.Metadata;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SuppressWarnings("unused")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FrameworkAutoConfiguration.class)
public class MetadataTest {
    @Autowired
    private FrameworkAutoConfiguration.KeyValueStoreProperties keyValueStoreProperties;

    @Autowired
    private Metadata metadata;

    @Test
    public void runtimeId() {
        System.out.println(metadata.getRuntimeId());
    }

    @Test
    public void properties() {
        Assert.assertEquals("_|_", keyValueStoreProperties.getFileSystem().getNameSeparator());
        Assert.assertEquals("TMP_KEY_VALUE", keyValueStoreProperties.getFileSystem().getWorkingRoot());
    }
}
