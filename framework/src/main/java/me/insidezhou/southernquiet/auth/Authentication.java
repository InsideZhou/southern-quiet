package me.insidezhou.southernquiet.auth;

import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.util.Set;

/**
 * 身份标识及权限信息。
 */
public interface Authentication extends Serializable {
    /**
     * 身份标识
     */
    @NonNull
    String getId();

    /**
     * 权限信息，默认情况下应该是ant-style模板。
     */
    @NonNull
    Set<String> getPermissionPatterns();
}
