package com.tambapps.pokemon.alakastats.infrastructure.cloud

import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RetryPolicyTest {

    private object NoJitter : Random() {
        override fun nextBits(bitCount: Int) = 0
    }

    @Test
    fun delaysDoubleAndAreTruncated() {
        val policy = RetryPolicy(baseDelay = 1.seconds, maxDelay = 32.seconds, random = NoJitter)

        val delays = (0..7).map { policy.delayBefore(it) }

        assertEquals(listOf(1, 2, 4, 8, 16, 32, 32, 32).map { it.seconds }, delays)
    }

    @Test
    fun stopsAfterMaxAttempts() = runTest {
        val sleeps = mutableListOf<Duration>()
        val policy = RetryPolicy(maxAttempts = 4, random = NoJitter, sleep = { sleeps.add(it) })
        var attempts = 0

        val result = policy.run(shouldRetry = { true }) { ++attempts }

        assertEquals(4, attempts)
        assertEquals(4, result)
        assertEquals(listOf(1, 2, 4).map { it.seconds }, sleeps)
    }

    @Test
    fun stopsAtTheFirstAcceptedResult() = runTest {
        val policy = RetryPolicy(random = NoJitter, sleep = {})
        var attempts = 0

        policy.run(shouldRetry = { it < 2 }) { ++attempts }

        assertEquals(2, attempts)
    }
}
