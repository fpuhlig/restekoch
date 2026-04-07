package com.restekoch.embedding

interface EmbeddingService {
    companion object {
        const val VECTOR_DIM = 768
        const val BATCH_SIZE = 250
    }

    fun embed(text: String): FloatArray

    fun embedBatch(texts: List<String>): List<FloatArray> {
        // default: call embed() one by one. Override for batch API support.
        return texts.map { embed(it) }
    }
}
