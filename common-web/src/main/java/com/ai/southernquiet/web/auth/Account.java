package com.ai.southernquiet.web.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * 跟{@link User}关联的账号。
 */
public interface Account extends Principal, Serializable {}
