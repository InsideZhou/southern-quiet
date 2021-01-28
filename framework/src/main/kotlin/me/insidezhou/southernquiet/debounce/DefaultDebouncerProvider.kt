package me.insidezhou.southernquiet.debounce

import kotlinx.coroutines.*
import me.insidezhou.southernquiet.FrameworkAutoConfiguration.DebounceProperties
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory
import org.aopalliance.intercept.MethodInvocation
import org.springframework.beans.factory.DisposableBean
import org.springframework.util.StringUtils
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class DefaultDebouncerProvider(properties: DebounceProperties) : DebouncerProvider, DisposableBean {
    private val debouncerAndInvocations = ConcurrentHashMap<String, DebouncerMetadata>()
    private val pendingInvocations = LinkedList<DebouncerMetadata>()
    private val reportDuration: Duration = properties.reportDuration
    private var reportTimer = System.currentTimeMillis()

    private val workingCounter = AtomicLong(0)
    private val putCounter = AtomicLong(0)

    private val scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
        {
            checkDebouncer()
            workDebouncer()
        },
        1,
        1,
        TimeUnit.MILLISECONDS)

    private val workCoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun getDebouncer(invocation: MethodInvocation, waitFor: Long, maxWaitFor: Long, name: String, executionTimeout: Long): Debouncer {
        var debouncerName = name

        val bean = invocation.getThis()
        val method = invocation.method
        if (StringUtils.isEmpty(debouncerName)) {
            debouncerName = bean.javaClass.name + "#" + method.name + "_" + waitFor + "_" + maxWaitFor
        }

        val metadata = debouncerAndInvocations.getOrPut(debouncerName) {
            log.message("准备生成debouncer")
                .context("name", debouncerName)
                .context("class", bean.javaClass.simpleName)
                .context("method", method.name)
                .context("waitFor", waitFor)
                .context("maxWaitFor", maxWaitFor)
                .context("timeout", executionTimeout)
                .debug()

            putCounter.incrementAndGet()

            DebouncerMetadata(debouncerName, DefaultDebouncer(waitFor, maxWaitFor), invocation, executionTimeout)
        }

        return metadata.debouncer
    }

    private fun checkDebouncer() {
        debouncerAndInvocations
            .filter { it.value.debouncer.isStable }
            .forEach {
                pendingInvocations.add(it.value)
                debouncerAndInvocations.remove(it.key)
            }

        workingCounter.addAndGet(pendingInvocations.size.toLong())

        val now = System.currentTimeMillis()
        val interval = Duration.ofMillis(now - reportTimer)
        if (interval >= reportDuration) {
            val count = putCounter.getAndSet(0)
            val unstable = debouncerAndInvocations.size

            reportTimer = now

            log.message("debouncer计数器")
                .context("count", count)
                .context("interval", interval)
                .context("unstable", unstable)
                .context("working", workingCounter.get())
                .trace()
        }
    }

    private fun workDebouncer() {
        do {
            val metadata = pendingInvocations.poll() ?: return

            workCoroutineScope.launch {
                val name = metadata.name
                val invocation = metadata.invocation
                val timeout = metadata.executionTimeout

                try {
                    if (timeout > 0) {
                        val result = withTimeoutOrNull(timeout) {
                            invocation.proceed()
                            "DONE"
                        }

                        if (null == result) {
                            log.message("施加了去抖动的方法执行超时")
                                .context("debouncer", name)
                                .warn()
                        }
                    }
                    else {
                        invocation.proceed()
                    }
                }
                catch (throwable: Throwable) {
                    log.message("施加了去抖动的方法执行失败")
                        .context("debouncer", name)
                        .exception(throwable)
                        .error()
                }
                finally {
                    workingCounter.decrementAndGet()
                }
            }
        }
        while (true)
    }

    override fun destroy() {
        scheduledFuture.cancel(true)
    }

    companion object {
        private val log = SouthernQuietLoggerFactory.getLogger(DefaultDebouncerProvider::class.java)
    }
}

private class DebouncerMetadata(val name: String, val debouncer: Debouncer, val invocation: MethodInvocation, val executionTimeout: Long)
