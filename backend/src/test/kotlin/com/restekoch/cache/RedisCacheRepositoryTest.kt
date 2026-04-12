package com.restekoch.cache

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class RedisCacheRepositoryTest {
    @Inject
    lateinit var repo: RedisCacheRepository

    @BeforeEach
    fun setUp() {
        // FLUSHDB to ensure test isolation between test methods
        repo.redisClient.send(
            io.vertx.mutiny.redis.client.Request.cmd(
                io.vertx.mutiny.redis.client.Command.create("FLUSHDB"),
            ),
        ).await().indefinitely()
        repo.createIndex()
    }

    @Test
    fun `create index does not throw`() {
        repo.createIndex()
    }

    @Test
    fun `store and search returns entry within threshold`() {
        val vector = FloatArray(768) { 0.1f }
        val entry =
            CacheEntry(
                ingredients = listOf("chicken", "rice"),
                recipesJson = """[{"id":"1","title":"Fried Rice"}]""",
                explanation = "Quick meal.",
            )
        repo.store(vector, entry, 3600)

        val result = repo.searchNearest(vector, 0.1)
        assertNotNull(result)
        assertEquals("Quick meal.", result!!.explanation)
    }

    @Test
    fun `search returns null when cache empty`() {
        val vector = FloatArray(768) { 0.5f }
        val result = repo.searchNearest(vector, 0.05)
        assertNull(result)
    }

    @Test
    fun `clear removes all entries`() {
        val vector = FloatArray(768) { 0.2f }
        val entry =
            CacheEntry(
                ingredients = listOf("egg"),
                recipesJson = "[]",
                explanation = "Test.",
            )
        repo.store(vector, entry, 3600)
        repo.clear()

        val result = repo.searchNearest(vector, 0.05)
        assertNull(result)
    }

    @Test
    fun `entry count returns zero for empty cache`() {
        assertEquals(0, repo.entryCount())
    }
}
