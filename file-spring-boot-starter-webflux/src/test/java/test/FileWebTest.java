package test;

import me.insidezhou.southernquiet.file.web.FileWebFluxAutoConfiguration;
import me.insidezhou.southernquiet.file.web.controller.FileWebController;
import me.insidezhou.southernquiet.file.web.model.FileInfo;
import me.insidezhou.southernquiet.file.web.model.ImageScale;
import me.insidezhou.southernquiet.filesystem.FileSystem;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

@SpringBootApplication
@ImportAutoConfiguration(FileWebFluxAutoConfiguration.class)
public class FileWebTest {
    public static void main(String[] args) {
        SpringApplication.run(FileWebTest.class);
    }

    @RestController
    public static class MainController extends FileWebController {
        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        public MainController(FileSystem fileSystem, FileWebFluxAutoConfiguration.Properties fileWebProperties, ServerProperties serverProperties) {
            super(fileSystem, fileWebProperties, serverProperties);
        }

        @PostMapping("upload")
        @Override
        public Flux<FileInfo> upload(Flux<FilePart> files, ServerHttpRequest request) {
            return super.upload(files, request);
        }

        @PostMapping("base64upload")
        @Override
        public Flux<FileInfo> base64upload(Flux<Part> files, ServerHttpRequest request) {
            return super.base64upload(files, request);
        }

        @GetMapping("file/{id}")
        @Override
        public ResponseEntity<Flux<DataBuffer>> file(@PathVariable String id, ServerHttpRequest request) throws IOException {
            return super.file(id, request);
        }

        @GetMapping("base64file/{id}")
        @Override
        public ResponseEntity<Mono<String>> base64file(@PathVariable String id, ServerHttpRequest request) throws IOException {
            return super.base64file(id, request);
        }

        @SuppressWarnings("MVCPathVariableInspection")
        @GetMapping(value = {"image/{id}/{scale}"})
        @Override
        public ResponseEntity<Flux<DataBuffer>> image(@PathVariable String id, ImageScale scale, ServerHttpRequest request, ServerHttpResponse response) throws IOException {
            return super.image(id, scale, request, response);
        }

        @GetMapping(value = {"image/{id}"})
        public ResponseEntity<Flux<DataBuffer>> image(@PathVariable String id, ServerHttpRequest request, ServerHttpResponse response) throws IOException {
            return super.image(id, null, request, response);
        }

        @SuppressWarnings("MVCPathVariableInspection")
        @ModelAttribute
        @Override
        protected ImageScale imageScale(@PathVariable(value = "scale", required = false) String scale) {
            return super.imageScale(scale);
        }
    }
}
