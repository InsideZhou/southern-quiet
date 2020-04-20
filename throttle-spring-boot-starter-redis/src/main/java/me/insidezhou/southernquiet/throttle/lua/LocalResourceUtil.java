package me.insidezhou.southernquiet.throttle.lua;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LocalResourceUtil {

    public static String getSource(String filePath) {
        ClassPathResource resource = new ClassPathResource(filePath);
        String source = null;
        try (InputStream resourceInputStream = resource.getInputStream()) {
            source = StreamUtils.copyToString(resourceInputStream, StandardCharsets.UTF_8);
        }
        catch (IOException ignored) {}
        return source;
    }

}
