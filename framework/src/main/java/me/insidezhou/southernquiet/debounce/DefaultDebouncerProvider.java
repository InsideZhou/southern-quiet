package me.insidezhou.southernquiet.debounce;

import me.insidezhou.southernquiet.FrameworkAutoConfiguration;
import me.insidezhou.southernquiet.logging.SouthernQuietLogger;
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory;
import me.insidezhou.southernquiet.util.Pair;
import me.insidezhou.southernquiet.util.Tuple;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

@SuppressWarnings("DuplicatedCode")
public class DefaultDebouncerProvider implements DebouncerProvider, DisposableBean {
    private final static SouthernQuietLogger log = SouthernQuietLoggerFactory.getLogger(DefaultDebouncerProvider.class);

    private final ConcurrentMap<String, Pair<Debouncer, MethodInvocation>> debouncerAndInvocations = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Tuple<String, Debouncer, MethodInvocation>> pendingInvocations = new ConcurrentLinkedQueue<>();

    private final Duration reportDuration;
    private long reportTimer = System.currentTimeMillis();

    private long checkCounter = 0;
    private final AtomicLong workCounter = new AtomicLong(0);

    private final ScheduledExecutorService executorService;

    public DefaultDebouncerProvider(FrameworkAutoConfiguration.DebounceProperties properties) {
        this.reportDuration = properties.getReportDuration();
        executorService = Executors.newScheduledThreadPool(properties.getWorkerCount() + 1);

        executorService.scheduleAtFixedRate(this::checkDebouncer, 1, 1, TimeUnit.MILLISECONDS);

        IntStream.range(0, properties.getWorkerCount()).forEach(i -> {
            executorService.scheduleAtFixedRate(this::invokeDebouncer, 1, 1, TimeUnit.MILLISECONDS);
        });
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

        long now = System.currentTimeMillis();
        Duration interval = Duration.ofMillis(now - reportTimer);
        if (interval.compareTo(reportDuration) >= 0) {
            long work = workCounter.getAndSet(0);

            log.message("debouncer计数器")
                .context("check", checkCounter)
                .context("work", work)
                .context("pending", pendingInvocations.size())
                .context("interval", interval)
                .debug();

            checkCounter = 0;
            reportTimer = now;
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

            workCounter.incrementAndGet();
            tuple = pendingInvocations.poll();
        }
    }

    @Override
    public void destroy() {
        executorService.shutdown();
    }
}
