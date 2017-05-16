package com.ai.southernquiet.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class CacheHelper {
    private static List<Pattern> invalidPatterns = new ArrayList<>();

    static {
        invalidPatterns.add(Pattern.compile("\\W+")); //目前只考虑数字、字母、下划线是合法的
    }

    public static List<Pattern> getInvalidPatterns() {
        return invalidPatterns;
    }

    public static void setInvalidPatterns(List<Pattern> invalidPatterns) {
        CacheHelper.invalidPatterns = invalidPatterns;
    }

    public static boolean isKeyValid(String key) {
        return !getInvalidPatterns().stream().anyMatch(p -> p.matcher(key).matches());
    }
}
