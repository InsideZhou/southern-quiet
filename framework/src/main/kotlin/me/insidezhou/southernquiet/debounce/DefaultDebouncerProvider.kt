package me.insidezhou.southernquiet.debounce

import kotlinx.coroutines.*
import me.insidezhou.southernquiet.FrameworkAutoConfiguration.DebounceProperties
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory
import me.insidezhou.southernquiet.util.Metadata
import org.aopalliance.intercept.MethodInvocation
import org.springframework.beans.factory.DisposableBean
import org.springframework.util.StringUtils
import java.time.Duration
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.round

@Suppress("MemberVisibilityCanBePrivate")
open class DefaultDebouncerProvider(properties: DebounceProperties, val dispatcher: CoroutineDispatcher) : DebouncerProvider, DisposableBean {
    constructor(properties: DebounceProperties, metadata: Metadata) : this(
        properties,
        ThreadPoolExecutor(
            metadata.coreNumber,
            Int.MAX_VALUE,
            maxOf(1L, round(metadata.coreNumber / 20.0).toLong()),
            TimeUnit.SECONDS,
            ArrayBlockingQueue<Runnable>(metadata.coreNumber * 30)
        ).asCoroutineDispatcher()
    )

    private val debouncerAndInvocations = ConcurrentHashMap<String, DebouncerMetadata>()
    private val pendingInvocations = LinkedList<DebouncerMetadata>()
    private val reportDuration: Duration = properties.reportDuration
    private var reportTimer = System.currentTimeMillis()

    private val pendingCounter = AtomicLong(0)
    private val putCounter = AtomicLong(0)
    private val doneCounter = AtomicLong(0)

    private val scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
        {
            checkDebouncer()
            workDebouncer()
        },
        1,
        1,
        TimeUnit.MILLISECONDS)

    private val workCoroutineScope = CoroutineScope(dispatcher)

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

        pendingCounter.addAndGet(pendingInvocations.size.toLong())

        val now = System.currentTimeMillis()
        val interval = Duration.ofMillis(now - reportTimer)
        if (interval >= reportDuration) {
            val put = putCounter.getAndSet(0)
            val done = doneCounter.getAndSet(0)
            val unstable = debouncerAndInvocations.size

            reportTimer = now

            val executor = dispatcher.asExecutor()

            log.message("debouncer计数器")
                .context {
                    it["put"] = put
                    it["done"] = done
                    it["interval"] = interval
                    it["unstable"] = unstable
                    it["pending"] = pendingCounter.get()

                    if (executor is ThreadPoolExecutor) {
                        it["working"] = executor.activeCount
                        it["pool"] = executor.poolSize
                        it["queue"] = executor.queue.size
                    }
                }
                .trace()
        }
    }

    private fun workDebouncer() {
        do {
            val metadata = pendingInvocations.poll() ?: return

            workCoroutineScope.launch {
                val invocation = metadata.invocation
                val timeout = metadata.executionTimeout

                try {
                    if (timeout > 0) {
                        val result = withTimeoutOrNull(timeout) {
                            invocation.proceed()
                            "DONE"
                        }

                        if (null == result) {
                            onWorkTimeout(metadata)
                        }
                    }
                    else {
                        invocation.proceed()
                    }
                }
                catch (throwable: Throwable) {
                    onWorkException(throwable, metadata)
                }
                finally {
                    pendingCounter.decrementAndGet()
                    doneCounter.incrementAndGet()
                }
            }
        }
        while (true)
    }

    protected open fun onWorkException(throwable: Throwable, metadata: DebouncerMetadata) {
        log.message("施加了去抖动的方法执行失败")
            .context("debouncer", metadata.name)
            .exception(throwable)
            .error()
    }

    protected open fun onWorkTimeout(metadata: DebouncerMetadata) {
        log.message("施加了去抖动的方法执行超时")
            .context("debouncer", metadata.name)
            .warn()
    }

    override fun destroy() {
        scheduledFuture.cancel(true)
    }

    companion object {
        private val log = SouthernQuietLoggerFactory.getLogger(DefaultDebouncerProvider::class.java)
    }
}

class DebouncerMetadata(val name: String, val debouncer: Debouncer, val invocation: MethodInvocation, val executionTimeout: Long)
