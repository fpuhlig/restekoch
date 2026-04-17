package com.restekoch.cache

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class ImageCacheRepositoryTest {
    @Inject
    lateinit var repo: ImageCacheRepository

    @Inject
    lateinit var redis: RedisDataSource

    @Inject
    lateinit var redisCacheRepository: RedisCacheRepository

    @BeforeEach
    fun setUp() {
        // flush everything to keep tests isolated
        redis.execute("FLUSHDB")
        redisCacheRepository.createIndex()
    }

    @Test
    fun `store and get round trip`() {
        repo.store("img:test:abc", """["egg","milk"]""", 3600)
        val value = repo.get("img:test:abc")
        assertNotNull(value)
        assertEquals("""["egg","milk"]""", value)
    }

    @Test
    fun `get returns null for missing key`() {
        assertNull(repo.get("img:test:missing"))
    }

    @Test
    fun `entry count reflects stored keys`() {
        repo.store("img:test:a", "[]", 3600)
        repo.store("img:test:b", "[]", 3600)
        assertEquals(2, repo.entryCount())
    }

    @Test
    fun `clear removes all img keys but leaves other prefixes`() {
        repo.store("img:test:a", "[]", 3600)
        redis.value(String::class.java).set("other:key", "keep-me")

        repo.clear()

        assertNull(repo.get("img:test:a"))
        assertEquals("keep-me", redis.value(String::class.java).get("other:key"))
    }
}
