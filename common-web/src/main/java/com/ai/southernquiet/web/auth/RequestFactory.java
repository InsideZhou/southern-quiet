package com.ai.southernquiet.web.auth;

import com.ai.southernquiet.web.CommonWebAutoConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RequestFactory<R extends Request> {
    Class<R> getRequestClass();

    R createInstance(HttpServletRequest httpServletRequest,
                     HttpServletResponse httpServletResponse,
                     CommonWebAutoConfiguration.SessionRememberMeProperties rememberMeProperties,
                     CommonWebAutoConfiguration.WebProperties webProperties,
                     AuthService authService);
}
