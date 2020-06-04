package me.insidezhou.southernquiet.debounce;

import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.util.Pair;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.*;

public class DefaultDebouncerProvider implements DebouncerProvider, DisposableBean {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(DefaultDebouncerProvider.class);

    private final ConcurrentMap<String, Pair<Debouncer, MethodInvocation>> debouncerAndInvocations = new ConcurrentHashMap<>();
    private final Future<?> future;

    public DefaultDebouncerProvider() {
        future = Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(this::invokeDebouncer, 7, 7, TimeUnit.MILLISECONDS);
    }

    @Override
    public Debouncer getDebouncer(MethodInvocation invocation, long waitFor, long maxWaitFor, String debouncerName) {
        Object bean = invocation.getThis();
        Method method = invocation.getMethod();

        if (StringUtils.isEmpty(debouncerName)) {
            debouncerName = bean.getClass().getName() + "#" + method.getName() + "_" + waitFor + "_" + maxWaitFor;
        }

        Pair<Debouncer, MethodInvocation> pair = debouncerAndInvocations.computeIfAbsent(debouncerName, (nm) -> new Pair<>(new DefaultDebouncer(waitFor, maxWaitFor), invocation));
        pair.setSecond(invocation);
        debouncerAndInvocations.put(debouncerName, pair);

        return pair.getFirst();
    }

    private void invokeDebouncer() {
        debouncerAndInvocations.forEach((name, pair) -> {
            Debouncer debouncer = pair.getFirst();
            MethodInvocation invocation = pair.getSecond();

            if (debouncer.isStable()) {
                try {
                    invocation.proceed();
                }
                catch (Throwable throwable) {
                    log.message("施加了去抖动的方法执行失败")
                        .context("debouncer", name)
                        .exception(throwable)
                        .error();
                }
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        future.cancel(true);
        future.get();
    }
}
