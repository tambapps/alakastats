package com.tambapps.pokemon.alakastats.infrastructure.cloud

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Truncated exponential backoff, as recommended by Google for 403 rate limit, 429 and 5xx errors:
 * waits min(base * 2^n + jitter, maxDelay) before the retry n.
 */
class RetryPolicy(
    val maxAttempts: Int = 5,
    private val baseDelay: Duration = 1.seconds,
    private val maxDelay: Duration = 32.seconds,
    private val random: Random = Random.Default,
    private val sleep: suspend (Duration) -> Unit = { delay(it) },
) {

    fun delayBefore(retry: Int): Duration {
        val exponential = baseDelay.inWholeMilliseconds * (1L shl min(retry, 20))
        val jitter = random.nextLong(0, 1000)
        return min(exponential + jitter, maxDelay.inWholeMilliseconds).milliseconds
    }

    suspend fun pause(retry: Int) = sleep(delayBefore(retry))

    /**
     * Runs [block] until it returns a result that [shouldRetry] rejects, or [maxAttempts] is reached
     */
    suspend fun <T> run(shouldRetry: (T) -> Boolean, block: suspend () -> T): T {
        var result = block()
        var retry = 0
        while (shouldRetry(result) && retry < maxAttempts - 1) {
            sleep(delayBefore(retry))
            retry++
            result = block()
        }
        return result
    }
}
