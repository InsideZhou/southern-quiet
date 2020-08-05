package test;

import me.insidezhou.southernquiet.file.web.controller.FileWebController;
import me.insidezhou.southernquiet.file.web.model.FileInfo;
import me.insidezhou.southernquiet.filesystem.FileSystem;
import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebFlux;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebTestClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SuppressWarnings("ConstantConditions")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FileWebTest.class)
@AutoConfigureWebFlux
@AutoConfigureWebTestClient
public class FileWebControllerTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ImportAutoConfiguration(classes = {
        WebFluxAutoConfiguration.class,
        WebTestClientAutoConfiguration.class
    })
    public static class Config {}

    private WebTestClient client;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private FileSystem fileSystem;

    @Autowired
    private ServerProperties serverProperties;

    private final FileSystemResource resource = new FileSystemResource("src/test/resources/test.png");
    private String base64EncodedFile;
    private String contextPath;

    @Before
    public void before() throws Exception {
        contextPath = serverProperties.getServlet().getContextPath();
        client = WebTestClient.bindToApplicationContext(applicationContext).build();

        byte[] data = StreamUtils.copyToByteArray(resource.getInputStream());
        base64EncodedFile = Base64.encodeBase64String(data);
    }

    @Test
    public void upload() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("files", resource, MediaType.IMAGE_PNG);

        uploadAssert(builder, "upload");
    }

    @Test
    public void base64upload() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("files", base64EncodedFile);

        uploadAssert(builder, "base64upload");
    }

    @SuppressWarnings("UnusedReturnValue")
    private FileInfo uploadAssert(MultipartBodyBuilder builder, String uri) {
        EntityExchangeResult<List<FileInfo>> result = client.post()
            .uri(contextPath + "/" + uri)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(builder.build())
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBodyList(FileInfo.class).hasSize(1)
            .returnResult();

        FileInfo fileInfo = result.getResponseBody().get(0);
        Assert.assertEquals(MediaType.IMAGE_PNG_VALUE, fileInfo.getContentType());
        Assert.assertEquals(contextPath + "/image/" + fileInfo.getId(), fileInfo.getUrl());

        return fileInfo;
    }

    @Test
    public void file() throws Exception {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("files", resource, MediaType.IMAGE_PNG);

        FileInfo fileInfo = uploadAssert(builder, "upload");

        EntityExchangeResult<byte[]> result = client.get()
            .uri("/file/{id}", fileInfo.getId())
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectHeader().contentLength(resource.contentLength())
            .expectHeader().contentTypeCompatibleWith(MediaType.IMAGE_PNG)
            .expectBody()
            .returnResult();

        String filePath = FileWebController.getFilePath(fileInfo.getId());

        fileSystem.put(filePath + "_file.png", new ByteArrayInputStream(result.getResponseBody()));
    }

    @Test
    public void base64file() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("files", resource, MediaType.IMAGE_PNG);

        FileInfo fileInfo = uploadAssert(builder, "upload");

        EntityExchangeResult<byte[]> result = client.get()
            .uri("/base64file/{id}", fileInfo.getId())
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody()
            .returnResult();

        Assert.assertEquals(base64EncodedFile, new String(result.getResponseBody(), StandardCharsets.UTF_8));
    }

    @Test
    public void image() throws Exception {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("files", resource, MediaType.IMAGE_PNG);

        FileInfo fileInfo = uploadAssert(builder, "upload");
        String etag = "\"" + fileInfo.getId() + "\"";


        EntityExchangeResult<byte[]> result = client.get()
            .uri("/image/{hash}", fileInfo.getId())
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectHeader().contentTypeCompatibleWith(MediaType.IMAGE_PNG)
            .expectHeader().valueMatches("etag", etag)
            .expectBody()
            .returnResult();

        String filePath = FileWebController.getFilePath(fileInfo.getId());

        fileSystem.put(filePath + "_image.png", new ByteArrayInputStream(result.getResponseBody()));
        Assert.assertTrue(result.getResponseHeaders().getContentLength() > 0);
    }

    @Test
    public void imageNotModified() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("files", resource, MediaType.IMAGE_PNG);

        FileInfo fileInfo = uploadAssert(builder, "upload");
        String etag = "\"" + fileInfo.getId() + "\"";

        client.get()
            .uri("/image/{hash}", fileInfo.getId())
            .headers(httpHeaders -> httpHeaders.setIfNoneMatch(etag))
            .exchange()
            .expectStatus().isNotModified();
    }
}
