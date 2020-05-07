package me.insidezhou.southernquiet.auth;

import org.springframework.lang.NonNull;

/**
 * 由下游代码使用，自定义其自身的身份及权限验证。
 */
public interface AuthProvider {
    /**
     * @param context 当前被检查的方法及目标对象。
     * @return 通过验证时必须有返回值。
     * @throws AuthException 验证未通过时必须抛出，可自行扩展及捕捉验证异常。
     */
    @NonNull
    Authentication getAuthentication(AuthContext context) throws AuthException;
}
