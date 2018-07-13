package test;

import com.ai.southernquiet.FrameworkAutoConfiguration;
import com.ai.southernquiet.broadcasting.Broadcaster;
import com.ai.southernquiet.util.Metadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@SuppressWarnings("unused")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FrameworkAutoConfiguration.class)
public class MetadataTest {
    @MockBean
    private Broadcaster<?> broadcaster;

    @Autowired
    private Metadata metadata;

    @Test
    public void runtimeId() {
        System.out.println(metadata.getRuntimeId());
    }
}
