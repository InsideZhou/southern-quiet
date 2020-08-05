package me.insidezhou.southernquiet.file.web.controller;

import me.insidezhou.southernquiet.file.web.FileWebFluxAutoConfiguration;
import me.insidezhou.southernquiet.file.web.exception.NotFoundException;
import me.insidezhou.southernquiet.file.web.model.FileInfo;
import me.insidezhou.southernquiet.file.web.model.ImageScale;
import me.insidezhou.southernquiet.filesystem.FileSystem;
import me.insidezhou.southernquiet.filesystem.InvalidFileException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.Tika;
import org.imgscalr.Scalr;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
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

@SuppressWarnings("DuplicatedCode")
public class FileWebController {
    public static String getFilePath(String filename) {
        int size = 3;
        String hash = DigestUtils.md5Hex(filename);

        return IntStream.range(0, size)
            .mapToObj(i -> hash.substring(i, i + size))
            .collect(Collectors.joining(FileSystem.PATH_SEPARATOR_STRING)) + FileSystem.PATH_SEPARATOR_STRING + filename;
    }

    protected final Tika tika = new Tika();

    protected final FileSystem fileSystem;
    protected final String contextPath;
    protected final FileWebFluxAutoConfiguration.Properties fileWebProperties;

    public FileWebController(FileSystem fileSystem,
                             FileWebFluxAutoConfiguration.Properties fileWebProperties,
                             ServerProperties serverProperties) {

        this.fileSystem = fileSystem;
        this.contextPath = serverProperties.getServlet().getContextPath();
        this.fileWebProperties = fileWebProperties;
    }

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

                if (mediaType.startsWith("image")) {
                    info.setUrl(builder.replacePath(contextPath + "/image/{hash}").build(hash).toString());
                }
                else {
                    info.setUrl(builder.replacePath(contextPath + "/file/{hash}").build(hash).toString());
                }

                return info;
            });
    }

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

                if (mediaType.startsWith("image")) {
                    info.setUrl(builder.replacePath(contextPath + "/image/{hash}").build(hash).toString());
                }
                else {
                    info.setUrl(builder.replacePath(contextPath + "/file/{hash}").build(hash).toString());
                }

                return info;
            });
    }

    public ResponseEntity<Flux<DataBuffer>> file(String id, ServerHttpRequest request) throws IOException {
        String path = getFilePath(id);
        if (!fileSystem.exists(path)) throw new NotFoundException();

        if (!request.getHeaders().getIfNoneMatch().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        InputStream resultStream;
        try (InputStream inputStream = fileSystem.openReadStream(path)) {
            resultStream = new ByteArrayInputStream(StreamUtils.copyToByteArray(inputStream));
        }
        catch (InvalidFileException e) {
            throw new IOException(e);
        }

        String contentType = tika.detect(resultStream);
        resultStream.reset();

        final InputStream finalResultStream = resultStream;

        return ResponseEntity.ok()
            .contentLength(resultStream.available())
            .contentType(MediaType.parseMediaType(contentType))
            .eTag(id)
            .body(DataBufferUtils.readInputStream(
                () -> finalResultStream,
                new DefaultDataBufferFactory(),
                (int) fileWebProperties.getBufferSize().toBytes()));
    }

    public ResponseEntity<Mono<String>> base64file(String id, ServerHttpRequest request) throws IOException {
        String path = getFilePath(id);
        if (!fileSystem.exists(path)) throw new NotFoundException();

        if (!request.getHeaders().getIfNoneMatch().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        String base64;
        try (InputStream inputStream = fileSystem.openReadStream(path)) {
            base64 = Base64.encodeBase64String(StreamUtils.copyToByteArray(inputStream));
        }
        catch (InvalidFileException e) {
            throw new IOException(e);
        }

        return ResponseEntity.ok()
            .contentLength(base64.length())
            .contentType(MediaType.TEXT_PLAIN)
            .eTag(id)
            .body(Mono.create(sink -> sink.success(base64)));
    }

    public ResponseEntity<Flux<DataBuffer>> image(String id, ImageScale scale, ServerHttpRequest request, ServerHttpResponse response) throws IOException {
        return image(id, scale, Scalr.Method.AUTOMATIC, request, response);
    }

    @SuppressWarnings("SameParameterValue")
    protected ResponseEntity<Flux<DataBuffer>> image(String id, ImageScale scale, Scalr.Method scaleMethod, ServerHttpRequest request, ServerHttpResponse response) throws IOException {
        String path = getFilePath(id);
        if (!fileSystem.exists(path)) throw new NotFoundException();

        if (!request.getHeaders().getIfNoneMatch().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        InputStream resultStream;
        try (InputStream inputStream = fileSystem.openReadStream(path)) {
            resultStream = new ByteArrayInputStream(StreamUtils.copyToByteArray(inputStream));
        }
        catch (InvalidFileException e) {
            throw new IOException(e);
        }

        String mediaType = tika.detect(resultStream);
        resultStream.reset();

        if (!StringUtils.hasText(mediaType) || !mediaType.startsWith("image")) throw new NotFoundException();

        if (null != scale) {
            BufferedImage image = Scalr.resize(
                ImageIO.read(resultStream),
                scaleMethod,
                Scalr.Mode.AUTOMATIC,
                scale.getWidth(),
                scale.getHeight()
            );

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                String subType = mediaType.split("/")[1];
                ImageIO.write(image, subType, outputStream);

                resultStream.close();
                resultStream = new ByteArrayInputStream(outputStream.toByteArray());
            }
        }

        final InputStream finalResultStream = resultStream;

        return ResponseEntity.ok()
            .contentLength(resultStream.available())
            .contentType(MediaType.parseMediaType(mediaType))
            .eTag(id)
            .body(DataBufferUtils.readInputStream(
                () -> finalResultStream,
                new DefaultDataBufferFactory(),
                (int) fileWebProperties.getBufferSize().toBytes()));
    }

    protected void saveFile(String filename, InputStream data) {
        try {
            data.reset();
            fileSystem.put(getFilePath(filename), data);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected ImageScale imageScale(String scale) {
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
