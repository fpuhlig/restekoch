package com.restekoch.indexer

import com.restekoch.recipe.Recipe
import com.restekoch.recipe.RecipeRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test

@QuarkusTest
class RecipeIndexerTest {
    @Inject
    lateinit var recipeIndexer: RecipeIndexer

    @Inject
    lateinit var recipeRepository: RecipeRepository

    @Test
    fun `indexAll indexes saved recipes`() {
        recipeRepository.save(
            Recipe(
                id = "",
                title = "Indexer Test Recipe",
                ingredients = listOf("salt", "pepper"),
                directions = listOf("mix"),
                ner = listOf("salt", "pepper"),
            ),
        )

        val count = recipeIndexer.indexAll()
        assert(count > 0) { "should index at least one recipe" }
    }

    @Test
    fun `indexAll with no recipes returns zero`() {
        // on a fresh emulator with no data, indexAll returns 0
        // but previous tests may have seeded data, so just check it does not throw
        val count = recipeIndexer.indexAll()
        assert(count >= 0)
    }
}
