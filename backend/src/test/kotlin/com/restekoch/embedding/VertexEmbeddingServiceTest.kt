package com.restekoch.embedding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VertexEmbeddingServiceTest {
    @Test
    fun `vector dimension constant is 768`() {
        assertEquals(768, EmbeddingService.VECTOR_DIM)
    }

    @Test
    fun `embed delegates to embedBatch`() {
        val service =
            object : EmbeddingService {
                var batchCalled = false

                override fun embed(text: String): FloatArray {
                    return embedBatch(listOf(text))[0]
                }

                override fun embedBatch(texts: List<String>): List<FloatArray> {
                    batchCalled = true
                    return texts.map { FloatArray(768) { 0.1f } }
                }
            }
        val result = service.embed("test")
        assertEquals(768, result.size)
        assertEquals(true, service.batchCalled)
    }
}
