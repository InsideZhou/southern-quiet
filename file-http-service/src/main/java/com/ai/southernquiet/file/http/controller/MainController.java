package com.ai.southernquiet.file.http.controller;

import com.ai.southernquiet.file.http.exception.NotFoundException;
import com.ai.southernquiet.file.http.model.FileInfo;
import com.ai.southernquiet.file.http.model.ImageScale;
import com.ai.southernquiet.filesystem.FileSystem;
import com.ai.southernquiet.filesystem.InvalidFileException;
import com.ai.southernquiet.util.AsyncRunner;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.Tika;
import org.apache.tika.io.IOUtils;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings({"SpringJavaAutowiredFieldsWarningInspection", "SpringJavaInjectionPointsAutowiringInspection", "MVCPathVariableInspection"})
@Controller
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

    public MainController(FileSystem fileSystem, AsyncRunner asyncRunner) {
        this.fileSystem = fileSystem;
        this.asyncRunner = asyncRunner;
    }

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @PostMapping("upload")
    @ResponseBody
    public List<FileInfo> upload(MultipartFile[] files) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString()).replaceQuery("");

        return Arrays.stream(files).map(f -> {
            String hash;
            String mediaType;

            InputStream inputStream;

            try {
                inputStream = new ByteArrayInputStream(f.getBytes());
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
            info.setUrl(builder.replacePath("file/{hash}").build(hash).toString());

            return info;
        }).collect(Collectors.toList());
    }

    @PostMapping("base64upload")
    @ResponseBody
    public List<FileInfo> base64upload(String[] files) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString()).replaceQuery("");

        return Arrays.stream(files).map(str -> {
            String hash;
            String mediaType;

            InputStream inputStream = new ByteArrayInputStream(Base64.decodeBase64(str));

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
            info.setUrl(builder.replacePath("base64file/{hash}").build(hash).toString());

            return info;
        }).collect(Collectors.toList());
    }

    @GetMapping("file/{id}")
    @ResponseBody
    public void file(@PathVariable String id) {
        String path = getFilePath(id);

        if (!fileSystem.exists(path)) throw new NotFoundException();

        try (InputStream inputStream = fileSystem.openReadStream(path)) {
            response.setHeader("Content-Type", tika.detect(inputStream));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (InputStream inputStream = fileSystem.openReadStream(path)) {
            IOUtils.copy(inputStream, response.getOutputStream());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("base64file/{id}")
    @ResponseBody
    public String base64file(@PathVariable String id) {
        String path = getFilePath(id);

        if (!fileSystem.exists(path)) throw new NotFoundException();

        try (InputStream inputStream = fileSystem.openReadStream(path)) {
            response.setHeader("Content-Type", tika.detect(inputStream));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (InputStream inputStream = fileSystem.openReadStream(path)) {
            return Base64.encodeBase64String(StreamUtils.copyToByteArray(inputStream));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("base64image/{id}/{scale}")
    @ResponseBody
    public String base64image(@PathVariable String id, ImageScale scale) {
        try (InputStream inputStream = getImage(id, scale)) {
            return Base64.encodeBase64String(StreamUtils.copyToByteArray(inputStream));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("image/{id}/{scale}")
    @ResponseBody
    public void image(@PathVariable String id, ImageScale scale) {
        try (InputStream inputStream = getImage(id, scale)) {
            IOUtils.copy(inputStream, response.getOutputStream());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream getImage(String id, ImageScale scale) throws IOException, InvalidFileException {
        String path = getFilePath(id);

        if (!fileSystem.exists(path)) throw new NotFoundException();

        String mediaType;

        try (InputStream inputStream = fileSystem.openReadStream(path)) {
            mediaType = tika.detect(inputStream);
        }

        if (!StringUtils.hasText(mediaType) || !mediaType.startsWith("image")) throw new NotFoundException();

        response.setHeader("Content-Type", mediaType);

        if (null == scale) {
            return fileSystem.openReadStream(path);
        }

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

        return fileSystem.openReadStream(scaledImagePath);
    }

    protected void saveFile(String filename, InputStream data) {
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
