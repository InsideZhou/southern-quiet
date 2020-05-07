package me.insidezhou.southernquiet.auth;

import java.io.Serializable;
import java.util.Set;

/**
 * 身份标识及权限信息。
 */
public class Authentication implements Serializable {
    /**
     * 身份标识
     */
    private String id;
    /**
     * 权限信息，默认情况下应该是ant-style模板。
     */
    private Set<String> permissionPatterns;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getPermissionPatterns() {
        return permissionPatterns;
    }

    public void setPermissionPatterns(Set<String> permissionPatterns) {
        this.permissionPatterns = permissionPatterns;
    }
}
