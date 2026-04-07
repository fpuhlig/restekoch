package com.restekoch.search

import com.restekoch.embedding.EmbeddingService
import com.restekoch.recipe.Recipe
import com.restekoch.recipe.RecipeRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class SearchService(
    val embeddingService: EmbeddingService,
    val redisVectorRepository: RedisVectorRepository,
    val recipeRepository: RecipeRepository,
) {
    fun search(
        ingredients: List<String>,
        limit: Int = 10,
    ): List<Recipe> {
        val queryText = ingredients.joinToString(", ")
        val queryVector = embeddingService.embed(queryText)
        val recipeIds = redisVectorRepository.searchSimilar(queryVector, limit)

        return recipeIds.mapNotNull { id ->
            recipeRepository.findById(id)
        }
    }
}
