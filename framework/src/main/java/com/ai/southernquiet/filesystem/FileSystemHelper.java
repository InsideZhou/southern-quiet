package com.ai.southernquiet.filesystem;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class FileSystemHelper {
    private static List<Pattern> invalidPatterns = new ArrayList<>();

    static {
        invalidPatterns.add(Pattern.compile("\\W+"));
    }

    public static List<Pattern> getInvalidPatterns() {
        return invalidPatterns;
    }

    public static void setInvalidPatterns(List<Pattern> invalidPatterns) {
        FileSystemHelper.invalidPatterns = invalidPatterns;
    }

    /**
     * @param filename 目前只考虑数字、字母、下划线是合法的
     */
    public static boolean isFileNameValid(String filename) {
        return getInvalidPatterns().stream().noneMatch(p -> p.matcher(filename).matches());
    }

    /**
     * @param filename 目前只考虑数字、字母、下划线是合法的
     */
    public static void assertFileNameValid(String filename) {
        Assert.isTrue(isFileNameValid(filename), "FileSystem接口的文件名只能是数字、字母、下划线");
    }
}
