package me.insidezhou.southernquiet.file.web.controller;

import me.insidezhou.southernquiet.file.web.exception.NotFoundException;
import me.insidezhou.southernquiet.file.web.model.FileInfo;
import me.insidezhou.southernquiet.file.web.model.ImageScale;
import me.insidezhou.southernquiet.filesystem.FileSystem;
import me.insidezhou.southernquiet.util.AsyncRunner;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.Tika;
import org.imgscalr.Scalr;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings({"WeakerAccess", "Duplicates"})
@RestController
public class MainController {
    public static String getFilePath(String filename) {
        int size = 3;
        String hash = DigestUtils.md5Hex(filename);

        return IntStream.range(0, size)
            .mapToObj(i -> hash.substring(i, i + size))
            .collect(Collectors.joining(FileSystem.PATH_SEPARATOR_STRING)) + FileSystem.PATH_SEPARATOR_STRING + filename;
    }

    @SuppressWarnings("WeakerAccess")
    public static String complementImagePath(String filePath, ImageScale scale) {
        return filePath + String.format("_%sX%s", scale.getWidth(), scale.getHeight());
    }

    @SuppressWarnings("unused")
    public static String getImagePath(String filename, ImageScale scale) {
        return complementImagePath(getFilePath(filename), scale);
    }

    private Tika tika = new Tika();

    private FileSystem fileSystem;
    private AsyncRunner asyncRunner;
    private String contextPath;

    public MainController(FileSystem fileSystem, AsyncRunner asyncRunner, ServerProperties serverProperties) {
        this.fileSystem = fileSystem;
        this.asyncRunner = asyncRunner;
        this.contextPath = serverProperties.getServlet().getContextPath();
    }

    @PostMapping("upload")
    public Flux<FileInfo> upload(Flux<FilePart> files, ServerHttpRequest request) {
        return files
            .map(part -> {

                Path tmpPath;
                try {
                    tmpPath = Files.createTempFile("", "");
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }

                File file = tmpPath.toFile();
                part.transferTo(file);

                return file;
            })
            .map(file -> {
                UriComponentsBuilder builder = UriComponentsBuilder.fromUri(request.getURI()).replaceQuery("");
                String hash;
                String mediaType;

                InputStream inputStream;

                try {
                    mediaType = tika.detect(file);
                    inputStream = new ByteArrayInputStream(FileCopyUtils.copyToByteArray(file));
                    hash = DigestUtils.sha256Hex(inputStream);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }

                saveFile(hash, inputStream);

                FileInfo info = new FileInfo();
                info.setId(hash);
                info.setContentType(mediaType);
                info.setUrl(builder.replacePath(contextPath + "/file/{hash}").build(hash).toString());

                return info;
            });
    }


    @PostMapping("base64upload")
    public Flux<FileInfo> base64upload(Flux<Part> files, ServerHttpRequest request) {
        return files
            .flatMap(Part::content)
            .map(dataBuffer -> {
                try {
                    return new ByteArrayInputStream(Base64.decodeBase64(StreamUtils.copyToByteArray(dataBuffer.asInputStream())));
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .map(inputStream -> {
                UriComponentsBuilder builder = UriComponentsBuilder.fromUri(request.getURI()).replaceQuery("");
                String hash;
                String mediaType;

                try {
                    mediaType = tika.detect(inputStream);
                    hash = DigestUtils.sha256Hex(inputStream);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }

                saveFile(hash, inputStream);

                FileInfo info = new FileInfo();
                info.setId(hash);
                info.setContentType(mediaType);
                info.setUrl(builder.replacePath(contextPath + "/file/{hash}").build(hash).toString());

                return info;
            });
    }

    @GetMapping("file/{id}")
    public Flux<DataBuffer> file(@PathVariable String id, ServerHttpResponse response) {
        String path = getFilePath(id);

        if (!fileSystem.exists(path)) throw new NotFoundException();

        return DataBufferUtils.readInputStream(
            () -> {
                InputStream resultStream;
                try (InputStream inputStream = fileSystem.openReadStream(path)) {
                    resultStream = new ByteArrayInputStream(StreamUtils.copyToByteArray(inputStream));
                }

                response.getHeaders().set("Content-Type", tika.detect(resultStream));
                resultStream.reset();

                response.getHeaders().set("Content-Length", String.valueOf(resultStream.available()));
                return resultStream;
            },
            new DefaultDataBufferFactory(),
            8192);
    }

    @GetMapping("base64file/{id}")
    public Mono<String> base64file(@PathVariable String id, ServerHttpResponse response) {
        String path = getFilePath(id);

        if (!fileSystem.exists(path)) throw new NotFoundException();

        return Mono.create(sink -> {
            response.getHeaders().set("Content-Type", MediaType.TEXT_PLAIN_VALUE);

            try (InputStream inputStream = fileSystem.openReadStream(path)) {
                String base64 = Base64.encodeBase64String(StreamUtils.copyToByteArray(inputStream));
                response.getHeaders().set("Content-Length", String.valueOf(base64.length()));
                sink.success(base64);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("MVCPathVariableInspection")
    @GetMapping(value = {"image/{id}/{scale}", "image/{id}"})
    public Flux<DataBuffer> image(@PathVariable String id, @RequestParam(required = false) ImageScale scale, ServerHttpResponse response) {
        String path = getFilePath(id);

        if (!fileSystem.exists(path)) throw new NotFoundException();

        return DataBufferUtils.readInputStream(
            () -> {
                InputStream resultStream;
                try (InputStream inputStream = fileSystem.openReadStream(path)) {
                    resultStream = new ByteArrayInputStream(StreamUtils.copyToByteArray(inputStream));
                }

                String mediaType = tika.detect(resultStream);
                if (!StringUtils.hasText(mediaType) || !mediaType.startsWith("image")) throw new NotFoundException();
                response.getHeaders().set("Content-Type", mediaType);

                if (null != scale) {
                    String scaledImagePath = complementImagePath(path, scale);

                    if (!fileSystem.exists(scaledImagePath)) {
                        try (InputStream inputStream = fileSystem.openReadStream(path)) {
                            BufferedImage image = Scalr.resize(
                                ImageIO.read(inputStream),
                                Scalr.Method.AUTOMATIC,
                                Scalr.Mode.AUTOMATIC,
                                scale.getWidth(),
                                scale.getHeight()
                            );

                            String subType = mediaType.split("/")[1];

                            try (OutputStream outputStream = fileSystem.openWriteStream(scaledImagePath)) {
                                ImageIO.write(image, subType, outputStream);
                            }
                        }
                    }

                    resultStream = fileSystem.openReadStream(scaledImagePath);
                }
                else {
                    resultStream.reset();
                }

                response.getHeaders().set("Content-Length", String.valueOf(resultStream.available()));
                return resultStream;
            },
            new DefaultDataBufferFactory(),
            8192);
    }

    private void saveFile(String filename, InputStream data) {
        asyncRunner.run(() -> {
            try {
                data.reset();
                fileSystem.put(getFilePath(filename), data);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings("MVCPathVariableInspection")
    @ModelAttribute
    public ImageScale imageScale(@PathVariable(value = "scale", required = false) String scale) {
        if (!StringUtils.hasText(scale)) return null;

        String[] values = scale.split("X");
        if (0 == values.length) return null;

        int width;
        try {
            width = Integer.parseInt(values[0]);
        }
        catch (NumberFormatException e) {
            return null;
        }

        @SuppressWarnings("SuspiciousNameCombination") int height = width;
        if (values.length > 1) {
            try {
                height = Integer.parseInt(values[1]);
            }
            catch (NumberFormatException e) {
                //pass
            }
        }

        ImageScale imageScale = new ImageScale();
        imageScale.setWidth(width);
        imageScale.setHeight(height);

        return imageScale;
    }
}
