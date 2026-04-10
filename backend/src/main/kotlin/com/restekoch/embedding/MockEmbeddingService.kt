package com.restekoch.embedding

import io.quarkus.arc.DefaultBean
import jakarta.enterprise.context.ApplicationScoped
import java.security.MessageDigest

/**
 * Deterministic fake embeddings for dev/test.
 * Same text always produces the same vector. No API call needed.
 * Active unless VertexEmbeddingService takes priority via restekoch.vertex.enabled=true.
 */
@ApplicationScoped
@DefaultBean
class MockEmbeddingService : EmbeddingService {
    override fun embed(text: String): FloatArray {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return FloatArray(EmbeddingService.VECTOR_DIM) { i ->
            val byte = digest[i % digest.size]
            byte.toFloat() / 128f
        }
    }
}
