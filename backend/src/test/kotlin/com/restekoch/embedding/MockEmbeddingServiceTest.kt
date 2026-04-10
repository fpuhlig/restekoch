package com.restekoch.embedding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MockEmbeddingServiceTest {
    private val service = MockEmbeddingService()

    @Test
    fun `returns correct vector dimensions`() {
        val result = service.embed("test")
        assertEquals(EmbeddingService.VECTOR_DIM, result.size)
    }

    @Test
    fun `same input produces same vector`() {
        val a = service.embed("chicken")
        val b = service.embed("chicken")
        assertEquals(a.toList(), b.toList())
    }

    @Test
    fun `different input produces different vector`() {
        val a = service.embed("chicken")
        val b = service.embed("pasta")
        assert(a.toList() != b.toList())
    }
}
