package me.insidezhou.southernquiet.file.web.model;

import org.springframework.util.StringUtils;

import java.util.Arrays;

public enum IdHashAlgorithm {

    sha256,
    sha1;

    public static IdHashAlgorithm getAlgorithm(String name) {
        if (StringUtils.isEmpty(name)) return sha256;
        try {
            return valueOf(name);
        }
        catch (IllegalArgumentException e) {
            return sha256;
        }
    }

    public static boolean isIdHashAlgorithm(String name) {
        try {
            return Arrays.stream(values()).anyMatch(idHashAlgorithm -> idHashAlgorithm.name().equals(name));
        }
        catch (Exception e) {
            return false;
        }
    }
}
