package me.insidezhou.southernquiet.debounce

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.insidezhou.southernquiet.FrameworkAutoConfiguration.DebounceProperties
import me.insidezhou.southernquiet.logging.SouthernQuietLoggerFactory
import me.insidezhou.southernquiet.util.Pair
import me.insidezhou.southernquiet.util.Tuple
import org.aopalliance.intercept.MethodInvocation
import org.springframework.beans.factory.DisposableBean
import org.springframework.util.StringUtils
import java.time.Duration
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong

class DefaultDebouncerProvider(properties: DebounceProperties) : DebouncerProvider, DisposableBean {
    private val debouncerAndInvocations: ConcurrentMap<String, Pair<Debouncer, MethodInvocation>> = ConcurrentHashMap()
    private val pendingInvocations = ConcurrentLinkedQueue<Tuple<String, Debouncer, MethodInvocation>>()
    private val reportDuration: Duration = properties.reportDuration
    private var reportTimer = System.currentTimeMillis()
    private var checkCounter: Long = 0
    private val workCounter = AtomicLong(0)

    private val checkFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({ checkDebouncer() }, 1, 1, TimeUnit.MILLISECONDS)
    private val workFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({ workDebouncer() }, 1, 1, TimeUnit.MILLISECONDS)
    private val workCoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun getDebouncer(invocation: MethodInvocation, waitFor: Long, maxWaitFor: Long, name: String): Debouncer {
        var debouncerName = name

        val bean = invocation.getThis()
        val method = invocation.method
        if (StringUtils.isEmpty(debouncerName)) {
            debouncerName = bean.javaClass.name + "#" + method.name + "_" + waitFor + "_" + maxWaitFor
        }
        val pair = debouncerAndInvocations.computeIfAbsent(debouncerName) { Pair(DefaultDebouncer(waitFor, maxWaitFor), invocation) }
        pair.second = invocation
        debouncerAndInvocations[debouncerName] = pair
        return pair.first
    }

    private fun checkDebouncer() {
        debouncerAndInvocations.keys
            .map {
                val pair = debouncerAndInvocations[it] ?: return@map null
                Tuple(it, pair.first, pair.second)
            }
            .filterNotNull()
            .filter { it.second.isStable }
            .forEach {
                ++checkCounter
                pendingInvocations.add(it)
                debouncerAndInvocations.remove(it.first)
            }

        val now = System.currentTimeMillis()
        val interval = Duration.ofMillis(now - reportTimer)
        if (interval >= reportDuration) {
            val check = checkCounter
            val work = workCounter.getAndSet(0)
            val pending = pendingInvocations.size

            checkCounter = 0
            reportTimer = now

            log.message("debouncer计数器")
                .context("check", check)
                .context("work", work)
                .context("pending", pending)
                .context("interval", interval)
                .debug()
        }
    }

    private fun workDebouncer() {
        do {
            val tuple = pendingInvocations.poll() ?: return

            workCoroutineScope.launch {
                val name = tuple.first
                val invocation = tuple.third
                try {
                    invocation.proceed()
                }
                catch (throwable: Throwable) {
                    log.message("施加了去抖动的方法执行失败")
                        .context("debouncer", name)
                        .exception(throwable)
                        .error()
                }
                workCounter.incrementAndGet()
            }
        }
        while (true)
    }

    override fun destroy() {
        checkFuture.cancel(true)
        workFuture.cancel(true)
    }

    companion object {
        private val log = SouthernQuietLoggerFactory.getLogger(DefaultDebouncerProvider::class.java)
    }
}
