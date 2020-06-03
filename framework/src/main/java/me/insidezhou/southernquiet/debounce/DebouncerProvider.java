package me.insidezhou.southernquiet.debounce;

import org.aopalliance.intercept.MethodInvocation;

public interface DebouncerProvider {
    Debouncer getDebouncer(MethodInvocation invocation, long waitFor, long maxWaitFor);
}
