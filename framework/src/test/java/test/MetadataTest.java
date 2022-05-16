package test;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.util.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

@SuppressWarnings("unused")
@ExtendWith(SpringExtension.class)
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
        Assertions.assertEquals("_|_", keyValueStoreProperties.getFileSystem().getNameSeparator());
        Assertions.assertEquals("TMP_KEY_VALUE", keyValueStoreProperties.getFileSystem().getWorkingRoot());
    }
}
