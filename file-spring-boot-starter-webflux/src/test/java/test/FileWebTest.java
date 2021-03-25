package test;

import me.insidezhou.southernquiet.file.web.FileWebFluxAutoConfiguration;
import me.insidezhou.southernquiet.file.web.controller.FileWebController;
import me.insidezhou.southernquiet.file.web.model.FileInfo;
import me.insidezhou.southernquiet.file.web.model.IdHashAlgorithm;
import me.insidezhou.southernquiet.file.web.model.ImageScale;
import me.insidezhou.southernquiet.filesystem.FileSystem;
import me.insidezhou.southernquiet.util.Metadata;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
@ImportAutoConfiguration(FileWebFluxAutoConfiguration.class)
public class FileWebTest {
    public static void main(String[] args) {
        SpringApplication.run(FileWebTest.class);
    }

    @RestController
    public static class MainController extends FileWebController {
        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        public MainController(FileSystem fileSystem, Metadata metadata, FileWebFluxAutoConfiguration.Properties fileWebProperties, ServerProperties serverProperties) {
            super(fileSystem, fileWebProperties, metadata, serverProperties);
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

        @GetMapping(value = {"file/{id}", "file/{id}/{hashAlgorithm}"})
        @Override
        public Mono<ResponseEntity<DataBuffer>> file(@PathVariable String id, @PathVariable(required = false) IdHashAlgorithm hashAlgorithm, ServerHttpRequest request) {
            return super.file(id, hashAlgorithm, request);
        }

        @GetMapping(value = {"base64file/{id}", "base64file/{id}/{hashAlgorithm}"})
        @Override
        public Mono<ResponseEntity<String>> base64file(@PathVariable String id, @PathVariable(required = false) IdHashAlgorithm hashAlgorithm, ServerHttpRequest request) {
            return super.base64file(id, hashAlgorithm, request);
        }

        @SuppressWarnings("MVCPathVariableInspection")
        @GetMapping(value = {"image/{id}/{scale}/{hashAlgorithm}"})
        @Override
        public Mono<ResponseEntity<DataBuffer>> image(@PathVariable String id, ImageScale scale, @PathVariable IdHashAlgorithm hashAlgorithm, ServerHttpRequest request, ServerHttpResponse response) {
            return super.image(id, scale, hashAlgorithm, request, response);
        }

        @GetMapping(value = {"image/{id}", "image/{id}/{scaleOrHashAlgorithm}"})
        public Mono<ResponseEntity<DataBuffer>> image(@PathVariable String id, @PathVariable(required = false) String scaleOrHashAlgorithm, ServerHttpRequest request, ServerHttpResponse response) {
            ImageScale imageScale = null;
            IdHashAlgorithm hashAlgorithm = null;
            if (StringUtils.hasText(scaleOrHashAlgorithm)) {
                if (IdHashAlgorithm.isIdHashAlgorithm(scaleOrHashAlgorithm)) {
                    hashAlgorithm = IdHashAlgorithm.getAlgorithm(scaleOrHashAlgorithm);
                }
                else {
                    imageScale = super.imageScale(scaleOrHashAlgorithm);
                }
            }
            return super.image(id, imageScale, hashAlgorithm, request, response);
        }

        @SuppressWarnings("MVCPathVariableInspection")
        @ModelAttribute
        @Override
        protected ImageScale imageScale(@PathVariable(value = "scale", required = false) String scale) {
            return super.imageScale(scale);
        }
    }
}
