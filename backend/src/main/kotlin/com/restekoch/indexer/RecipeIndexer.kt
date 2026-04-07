package com.restekoch.indexer

import com.restekoch.embedding.EmbeddingService
import com.restekoch.recipe.RecipeRepository
import com.restekoch.search.RedisVectorRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class RecipeIndexer(
    val recipeRepository: RecipeRepository,
    val embeddingService: EmbeddingService,
    val redisVectorRepository: RedisVectorRepository,
) {
    fun indexAll(): Int {
        redisVectorRepository.createIndex()

        val recipes = recipeRepository.findAll(limit = 2000, offset = 0)
        val toIndex = recipes.filter { it.ner.isNotEmpty() }

        // batch embed in chunks of 250 (Vertex AI limit)
        var indexed = 0
        toIndex.chunked(EmbeddingService.BATCH_SIZE).forEach { chunk ->
            val texts = chunk.map { it.ner.joinToString(", ") }
            val vectors = embeddingService.embedBatch(texts)

            chunk.zip(vectors).forEach { (recipe, vector) ->
                val ner = recipe.ner.joinToString(", ")
                redisVectorRepository.storeRecipeVector(recipe.id, ner, vector)
                indexed++
            }
        }

        return indexed
    }
}
