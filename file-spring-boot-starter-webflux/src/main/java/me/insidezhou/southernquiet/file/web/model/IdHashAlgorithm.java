package me.insidezhou.southernquiet.file.web.model;

import org.springframework.util.StringUtils;

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
}
