package me.insidezhou.southernquiet.debounce;

import org.aopalliance.intercept.MethodInvocation;

public interface DebouncerProvider {
    default Debouncer getDebouncer(MethodInvocation invocation, long waitFor, long maxWaitFor) {
        return getDebouncer(invocation, waitFor, maxWaitFor, null);
    }

    default Debouncer getDebouncer(MethodInvocation invocation, long waitFor, long maxWaitFor, String debouncerName) {
        return getDebouncer(invocation, waitFor, maxWaitFor, debouncerName, -1);
    }

    Debouncer getDebouncer(MethodInvocation invocation, long waitFor, long maxWaitFor, String debouncerName, long executionTimeout);
}
