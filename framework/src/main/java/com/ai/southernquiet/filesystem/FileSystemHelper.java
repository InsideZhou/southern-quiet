package com.ai.southernquiet.filesystem;

import org.springframework.util.Assert;

import java.util.regex.Pattern;

public abstract class FileSystemHelper {
    /**
     * 默认的文件名格式，可以被外部配置覆盖。
     *
     * @see com.ai.southernquiet.FrameworkAutoConfiguration.FileSystemProperties#nameRegex
     */
    private static Pattern namePattern = Pattern.compile("^[\\w\\-]+(\\.?[\\w\\-])*$");

    public static Pattern getNamePattern() {
        return namePattern;
    }

    public static void setNamePattern(Pattern namePattern) {
        FileSystemHelper.namePattern = namePattern;
    }

    public static boolean isFileNameValid(String filename) {
        return namePattern.matcher(filename).matches();
    }

    public static void assertFileNameValid(String filename) {
        Assert.isTrue(isFileNameValid(filename), "非法的FileSystem文件名格式");
    }
}
