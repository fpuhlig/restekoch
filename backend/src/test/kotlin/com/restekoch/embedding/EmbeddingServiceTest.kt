package com.restekoch.embedding

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

@QuarkusTest
class EmbeddingServiceTest {
    @Inject
    lateinit var embeddingService: EmbeddingService

    @Test
    fun `embed returns vector with correct dimensions`() {
        val result = embeddingService.embed("test input")
        assertEquals(EmbeddingService.VECTOR_DIM, result.size)
    }

    @Test
    fun `embed is deterministic for same input`() {
        val first = embeddingService.embed("chicken tomatoes")
        val second = embeddingService.embed("chicken tomatoes")
        assertEquals(first.toList(), second.toList())
    }

    @Test
    fun `embed produces different vectors for different input`() {
        val a = embeddingService.embed("chicken")
        val b = embeddingService.embed("pasta")
        assertNotNull(a)
        assertNotNull(b)
        assert(a.toList() != b.toList())
    }

    @Test
    fun `embedBatch returns correct number of vectors`() {
        val results = embeddingService.embedBatch(listOf("a", "b", "c"))
        assertEquals(3, results.size)
        results.forEach { assertEquals(EmbeddingService.VECTOR_DIM, it.size) }
    }
}
