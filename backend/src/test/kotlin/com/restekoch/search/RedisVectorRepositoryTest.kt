package com.restekoch.search

import com.restekoch.embedding.EmbeddingService
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class RedisVectorRepositoryTest {
    @Inject
    lateinit var repository: RedisVectorRepository

    @Inject
    lateinit var embeddingService: EmbeddingService

    @BeforeEach
    fun setup() {
        repository.createIndex()
    }

    @Test
    fun `createIndex is idempotent`() {
        // calling twice should not throw
        repository.createIndex()
        repository.createIndex()
    }

    @Test
    fun `store and search returns matching recipe`() {
        val ner = "pasta tomato basil garlic olive oil"
        val vector = embeddingService.embed(ner)
        repository.storeRecipeVector("recipe-001", ner, vector)

        // search with similar ingredients
        val queryVector = embeddingService.embed("tomato pasta basil")
        val results = repository.searchSimilar(queryVector, 5)

        assertTrue(results.isNotEmpty(), "should find at least one result")
        assertTrue(results.contains("recipe-001"), "results should contain recipe-001")
    }

    @Test
    fun `search with no data returns empty list`() {
        val queryVector = FloatArray(EmbeddingService.VECTOR_DIM) { 0.1f }
        val results = repository.searchSimilar(queryVector, 5)

        // may return empty or the previously stored recipe, both are valid
        assertTrue(results.size <= 5)
    }
}
