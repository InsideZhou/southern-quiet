package me.insidezhou.southernquiet.file.web.controller;

import me.insidezhou.southernquiet.file.web.FileWebFluxAutoConfiguration;
import me.insidezhou.southernquiet.file.web.exception.NotFoundException;
import me.insidezhou.southernquiet.file.web.model.FileInfo;
import me.insidezhou.southernquiet.file.web.model.IdHashAlgorithm;
import me.insidezhou.southernquiet.file.web.model.ImageScale;
import me.insidezhou.southernquiet.filesystem.FileSystem;
import me.insidezhou.southernquiet.filesystem.InvalidFileException;
import me.insidezhou.southernquiet.util.Metadata;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.Tika;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SuppressWarnings({"DuplicatedCode", "BlockingMethodInNonBlockingContext"})
public class FileWebController implements DisposableBean {
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

    protected DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory(true);
    protected Scheduler reactorScheduler;

    public FileWebController(FileSystem fileSystem,
                             FileWebFluxAutoConfiguration.Properties fileWebProperties,
                             Metadata metadata,
                             ServerProperties serverProperties) {
        this(fileSystem, fileWebProperties, serverProperties, Schedulers.fromExecutorService(new ThreadPoolExecutor(
            metadata.getCoreNumber(),
            metadata.getCoreNumber() * 8,
            Math.min(30, metadata.getCoreNumber()),
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(metadata.getCoreNumber() * 8)
        )));
    }

    public FileWebController(FileSystem fileSystem,
                             FileWebFluxAutoConfiguration.Properties fileWebProperties,
                             ServerProperties serverProperties,
                             Scheduler scheduler) {

        this.fileSystem = fileSystem;
        this.contextPath = serverProperties.getServlet().getContextPath();
        this.fileWebProperties = fileWebProperties;
        this.reactorScheduler = scheduler;
    }

    public String getFileLinkPath(String filename) {
        String linkPathPrefix = fileWebProperties.getLinkPathPrefix();
        if (StringUtils.hasText(linkPathPrefix)) {
            if (!linkPathPrefix.endsWith(FileSystem.PATH_SEPARATOR_STRING)) {
                linkPathPrefix = linkPathPrefix + FileSystem.PATH_SEPARATOR_STRING;
            }
            return linkPathPrefix + getFilePath(filename);
        }
        else {
            return getFilePath(filename);
        }
    }

    public Flux<FileInfo> upload(Flux<FilePart> files, ServerHttpRequest request) {
        return files
            .publishOn(reactorScheduler)
            .map(part -> partToInputStream(part, false))
            .map(inputStream -> processUploadStream(inputStream, request));
    }

    public Flux<FileInfo> base64upload(Flux<Part> files, ServerHttpRequest request) {
        return files
            .publishOn(reactorScheduler)
            .map(part -> partToInputStream(part, true))
            .map(inputStream -> processUploadStream(inputStream, request));
    }

    public Mono<ResponseEntity<DataBuffer>> file(String id, IdHashAlgorithm hashAlgorithm, ServerHttpRequest request) {
        return Mono.just(ResponseEntity.ok())
            .publishOn(reactorScheduler)
            .handle((okResponseBuilder, sink) -> {
                String path;
                if (IdHashAlgorithm.sha1.equals(hashAlgorithm)) {
                    path = getFileLinkPath(id);
                }
                else {
                    path = getFilePath(id);
                }

                if (!fileSystem.exists(path)) {
                    sink.error(new NotFoundException());
                    return;
                }

                if (!request.getHeaders().getIfNoneMatch().isEmpty()) {
                    sink.next(ResponseEntity.status(HttpStatus.NOT_MODIFIED).build());
                    return;
                }

                InputStream resultStream;
                try (InputStream inputStream = fileSystem.openReadStream(path)) {
                    resultStream = new ByteArrayInputStream(StreamUtils.copyToByteArray(inputStream));
                }
                catch (InvalidFileException e) {
                    sink.error(new IOException(e));
                    return;
                }
                catch (IOException e) {
                    sink.error(e);
                    return;
                }

                try {
                    String contentType = tika.detect(resultStream);
                    resultStream.reset();

                    ResponseEntity<DataBuffer> responseEntity = okResponseBuilder
                        .contentLength(resultStream.available())
                        .contentType(MediaType.parseMediaType(contentType))
                        .eTag(id)
                        .body(dataBufferFactory.wrap(StreamUtils.copyToByteArray(resultStream)));

                    sink.next(responseEntity);
                }
                catch (IOException e) {
                    sink.error(e);
                }
            });
    }

    public Mono<ResponseEntity<String>> base64file(String id, IdHashAlgorithm hashAlgorithm, ServerHttpRequest request) {
        return Mono.just(ResponseEntity.ok())
            .publishOn(reactorScheduler)
            .handle((okResponseBuilder, sink) -> {
                String path;
                if (IdHashAlgorithm.sha1.equals(hashAlgorithm)) {
                    path = getFileLinkPath(id);
                }
                else {
                    path = getFilePath(id);
                }

                if (!fileSystem.exists(path)) {
                    sink.error(new NotFoundException());
                    return;
                }

                if (!request.getHeaders().getIfNoneMatch().isEmpty()) {
                    sink.next(ResponseEntity.status(HttpStatus.NOT_MODIFIED).build());
                    return;
                }

                String base64;
                try (InputStream inputStream = fileSystem.openReadStream(path)) {
                    base64 = Base64.encodeBase64String(StreamUtils.copyToByteArray(inputStream));
                }
                catch (InvalidFileException e) {
                    sink.error(new IOException(e));
                    return;
                }
                catch (IOException e) {
                    sink.error(e);
                    return;
                }

                ResponseEntity<String> responseEntity = okResponseBuilder
                    .contentLength(base64.length())
                    .contentType(MediaType.TEXT_PLAIN)
                    .eTag(id)
                    .body(base64);

                sink.next(responseEntity);
            });
    }

    public Mono<ResponseEntity<DataBuffer>> image(String id, ImageScale scale, IdHashAlgorithm hashAlgorithm, ServerHttpRequest request, ServerHttpResponse response) {
        return image(id, scale, hashAlgorithm, Scalr.Method.AUTOMATIC, request, response);
    }

    @SuppressWarnings({"SameParameterValue", "unused"})
    protected Mono<ResponseEntity<DataBuffer>> image(String id, ImageScale scale, IdHashAlgorithm hashAlgorithm, Scalr.Method scaleMethod, ServerHttpRequest request, ServerHttpResponse response) {
        return Mono.just(ResponseEntity.ok())
            .publishOn(reactorScheduler)
            .handle((okResponseBuilder, sink) -> {
                String path;
                if (IdHashAlgorithm.sha1.equals(hashAlgorithm)) {
                    path = getFileLinkPath(id);
                }
                else {
                    path = getFilePath(id);
                }
                if (!fileSystem.exists(path)) {
                    sink.error(new NotFoundException());
                    return;
                }

                if (!request.getHeaders().getIfNoneMatch().isEmpty()) {
                    sink.next(ResponseEntity.status(HttpStatus.NOT_MODIFIED).build());
                    return;
                }

                InputStream resultStream;
                try (InputStream inputStream = fileSystem.openReadStream(path)) {
                    resultStream = new ByteArrayInputStream(StreamUtils.copyToByteArray(inputStream));
                }
                catch (InvalidFileException e) {
                    sink.error(new IOException(e));
                    return;
                }
                catch (IOException e) {
                    sink.error(e);
                    return;
                }

                try {
                    String mediaType = tika.detect(resultStream);
                    resultStream.reset();

                    if (!StringUtils.hasText(mediaType) || !mediaType.startsWith("image")) {
                        sink.error(new NotFoundException());
                        return;
                    }

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

                    ResponseEntity<DataBuffer> responseEntity = okResponseBuilder
                        .contentLength(resultStream.available())
                        .contentType(MediaType.parseMediaType(mediaType))
                        .eTag(id)
                        .body(dataBufferFactory.wrap(StreamUtils.copyToByteArray(resultStream)));

                    sink.next(responseEntity);
                }
                catch (IOException e) {
                    sink.error(e);
                }
            });
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

    protected void saveSymbolicLink(String filename, ServerHttpRequest request, InputStream inputStream) {
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        if (queryParams.getFirst("link") == null || !"sha1".equals(queryParams.getFirst("link"))) return;

        try {
            inputStream.reset();
            String link = DigestUtils.sha1Hex(inputStream);
            fileSystem.createSymbolicLink(getFileLinkPath(link), getFilePath(filename));
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

    protected InputStream partToInputStream(Part part, boolean base64Decode) {
        Stream<InputStream> stream = part.content().map(DataBuffer::asInputStream).toStream();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        stream.forEach(inputStream -> {
            try {
                StreamUtils.copy(inputStream, outputStream);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return new ByteArrayInputStream(base64Decode ? Base64.decodeBase64(outputStream.toByteArray()) : outputStream.toByteArray());
    }

    protected FileInfo processUploadStream(InputStream inputStream, ServerHttpRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(request.getURI()).replaceQuery("");
        String hash;
        String mediaType;

        try {
            mediaType = tika.detect(inputStream);
            inputStream.reset();
            hash = DigestUtils.sha256Hex(inputStream);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        saveFile(hash, inputStream);
        saveSymbolicLink(hash, request, inputStream);

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
    }

    @Override
    public void destroy() {
        reactorScheduler.dispose();
    }
}
