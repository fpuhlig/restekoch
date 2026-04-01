package com.restekoch.recipe

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class RecipeService(
    val repository: RecipeRepository,
) {
    fun findAll(
        limit: Int = 20,
        offset: Int = 0,
    ): List<Recipe> = repository.findAll(limit, offset)

    fun findById(id: String): Recipe? = repository.findById(id)

    fun save(recipe: Recipe): String = repository.save(recipe)

    fun saveAll(recipes: List<Recipe>) = repository.saveAll(recipes)
}
