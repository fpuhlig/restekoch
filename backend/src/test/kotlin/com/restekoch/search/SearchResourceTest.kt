package com.restekoch.search

import com.restekoch.indexer.RecipeIndexer
import com.restekoch.recipe.Recipe
import com.restekoch.recipe.RecipeRepository
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchResourceTest {
    @Inject
    lateinit var recipeRepository: RecipeRepository

    @Inject
    lateinit var recipeIndexer: RecipeIndexer

    @BeforeAll
    fun setup() {
        // seed test recipes
        recipeRepository.save(
            Recipe(
                id = "",
                title = "Tomato Pasta",
                ingredients = listOf("pasta", "tomato sauce", "garlic"),
                directions = listOf("boil pasta", "add sauce"),
                ner = listOf("pasta", "tomato", "garlic"),
            ),
        )
        recipeRepository.save(
            Recipe(
                id = "",
                title = "Garlic Bread",
                ingredients = listOf("bread", "butter", "garlic"),
                directions = listOf("spread butter", "toast"),
                ner = listOf("bread", "butter", "garlic"),
            ),
        )

        // index recipes into Redis
        recipeIndexer.indexAll()
    }

    @Test
    fun `search returns matching recipes`() {
        given()
            .queryParam("ingredients", "pasta,tomato")
            .`when`()
            .get("/api/search")
            .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
    }

    @Test
    fun `search with empty ingredients returns empty list`() {
        given()
            .queryParam("ingredients", "")
            .`when`()
            .get("/api/search")
            .then()
            .statusCode(200)
            .body("size()", `is`(0))
    }

    @Test
    fun `search respects limit parameter`() {
        given()
            .queryParam("ingredients", "garlic")
            .queryParam("limit", "1")
            .`when`()
            .get("/api/search")
            .then()
            .statusCode(200)
            .body("size()", `is`(1))
    }
}
