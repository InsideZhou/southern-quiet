package me.insidezhou.southernquiet.debounce;

import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.util.Pair;
import me.insidezhou.southernquiet.util.Tuple;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;

public class DefaultDebouncerProvider implements DebouncerProvider, DisposableBean {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(DefaultDebouncerProvider.class);

    private final ConcurrentMap<String, Pair<Debouncer, MethodInvocation>> debouncerAndInvocations = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Tuple<String, Debouncer, MethodInvocation>> pendingInvocations = new ConcurrentLinkedQueue<>();
    private final Future<?> checkFuture;
    private final Future<?> invokeFuture;

    private long checkCounter;
    private long invokeCounter;

    private final Duration checkReportDuration = Duration.ofSeconds(1);
    private final Duration invokeReportDuration = Duration.ofSeconds(1);

    private Instant checkTimer;
    private Instant invokeTimer;

    public DefaultDebouncerProvider() {
        checkFuture = Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(this::checkDebouncer, 1, 1, TimeUnit.MILLISECONDS);

        checkTimer = Instant.now();

        invokeFuture = Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(this::invokeDebouncer, 1, 1, TimeUnit.MILLISECONDS);

        invokeTimer = Instant.now();
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

    private void checkDebouncer() {
        Arrays.stream(debouncerAndInvocations.keySet().toArray(new String[0]))
            .map(name -> {
                Pair<Debouncer, MethodInvocation> pair = debouncerAndInvocations.get(name);
                if (null == pair) return null;

                Debouncer debouncer = pair.getFirst();
                MethodInvocation invocation = pair.getSecond();

                return new Tuple<>(name, debouncer, invocation);
            })
            .filter(Objects::nonNull)
            .filter(tuple -> tuple.getSecond().isStable())
            .forEach(tuple -> {
                ++checkCounter;
                pendingInvocations.add(tuple);
                debouncerAndInvocations.remove(tuple.getFirst());
            });

        Instant now = Instant.now();
        if (now.minus(checkReportDuration).isAfter(checkTimer)) {
            log.message("debouncer稳定检查计数器")
                .context("count", checkCounter)
                .context("duration", Duration.between(invokeTimer, now))
                .debug();

            checkCounter = 0;
            checkTimer = now;
        }
    }

    private void invokeDebouncer() {
        Tuple<String, Debouncer, MethodInvocation> tuple = pendingInvocations.poll();
        while (null != tuple) {
            String name = tuple.getFirst();
            MethodInvocation invocation = tuple.getThird();

            try {
                invocation.proceed();
            }
            catch (Throwable throwable) {
                log.message("施加了去抖动的方法执行失败")
                    .context("debouncer", name)
                    .exception(throwable)
                    .error();
            }

            ++invokeCounter;
            tuple = pendingInvocations.poll();
        }

        Instant now = Instant.now();
        if (now.minus(invokeReportDuration).isAfter(invokeTimer)) {
            log.message("debouncer执行计数器")
                .context("count", invokeCounter)
                .context("duration", Duration.between(invokeTimer, now))
                .debug();

            invokeCounter = 0;
            invokeTimer = now;
        }
    }

    @Override
    public void destroy() throws Exception {
        checkFuture.cancel(true);
        checkFuture.get();

        invokeFuture.cancel(true);
        invokeFuture.get();
    }
}
