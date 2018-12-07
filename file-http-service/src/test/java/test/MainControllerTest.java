package test;

import com.ai.southernquiet.file.http.controller.MainController;
import com.ai.southernquiet.file.http.model.FileInfo;
import com.ai.southernquiet.filesystem.FileSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.tika.mime.MediaType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FileHttpServiceTest.class)
@AutoConfigureMockMvc
public class MainControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private FileSystem fileSystem;

    @Autowired
    private ObjectMapper objectMapper;

    private InputStream inputStream;

    private String base64EncodedFile;

    @Before
    public void before() throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/test.png"));
        base64EncodedFile = Base64.encodeBase64String(data);
        inputStream = new ByteArrayInputStream(data);
    }

    @Test
    public void upload() throws Exception {
        uploadAssert(uploadFile());
    }

    @Test
    public void base64upload() throws Exception {
        uploadAssert(base64uploadFile());
    }

    private void uploadAssert(MvcResult result) throws Exception {
        FileInfo[] fileInfoArray = objectMapper.readValue(result.getResponse().getContentAsString(), FileInfo[].class);

        String filePath = MainController.getFilePath(fileInfoArray[0].getId());

        Arrays.stream(fileInfoArray).forEach(info -> {
            Assert.assertEquals("image/png", info.getContentType());
            Assert.assertTrue(info.getUrl().startsWith("//"));
            System.out.println(String.format("%s|%s", info.getId(), info.getUrl()));
        });

        Assert.assertTrue(fileSystem.exists(filePath));
        inputStream.reset();
        Assert.assertTrue(fileSystem.meta(filePath).getSize() == inputStream.available());
    }

    @Test
    public void file() throws Exception {
        MvcResult result = uploadFile();

        FileInfo fileInfo = objectMapper.readValue(result.getResponse().getContentAsString(), FileInfo[].class)[0];

        result = mvc.perform(
            MockMvcRequestBuilders.get("/file/{hash}", fileInfo.getId())
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string("Content-Type", "image/png"))
            .andReturn();

        String filePath = MainController.getFilePath(fileInfo.getId());

        fileSystem.put(filePath + "_file.png", new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));

        Assert.assertTrue(result.getResponse().getContentAsByteArray().length > 0);
    }

    @Test
    public void image() throws Exception {
        MvcResult result = uploadFile();

        FileInfo fileInfo = objectMapper.readValue(result.getResponse().getContentAsString(), FileInfo[].class)[0];

        result = mvc.perform(
            MockMvcRequestBuilders.get("/image/{hash}/300", fileInfo.getId())
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string("Content-Type", "image/png"))
            .andReturn();

        String filePath = MainController.getFilePath(fileInfo.getId());

        fileSystem.put(filePath + "_image.png", new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));

        Assert.assertTrue(result.getResponse().getContentAsByteArray().length > 0);
    }

    private MvcResult uploadFile() throws Exception {
        Assert.assertNotNull(inputStream);

        MockMultipartFile mockFile = new MockMultipartFile(
            "files",
            "test.png",
            MediaType.image("png").toString(),
            inputStream
        );

        return mvc.perform(
            MockMvcRequestBuilders.multipart("/upload")
                .file(mockFile)
                .file(mockFile)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
    }

    private MvcResult base64uploadFile() throws Exception {
        Assert.assertNotNull(inputStream);

        return mvc.perform(
            MockMvcRequestBuilders.post("/base64upload")
                .param("files", base64EncodedFile, base64EncodedFile)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
    }
}
