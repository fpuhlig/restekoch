package com.restekoch.recipe

import com.google.cloud.firestore.Firestore
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
class RecipeResourceTest {
    @Inject
    lateinit var firestore: Firestore

    @Inject
    lateinit var service: RecipeService

    @BeforeEach
    fun cleanup() {
        val docs = firestore.collection("recipes").get().get().documents
        val batch = firestore.batch()
        docs.forEach { batch.delete(it.reference) }
        if (docs.isNotEmpty()) batch.commit().get()
    }

    @Test
    fun listReturnsEmptyWhenNoRecipes() {
        given()
            .`when`().get("/api/recipes")
            .then()
            .statusCode(200)
            .body("size()", `is`(0))
    }

    @Test
    fun listReturnsRecipesAfterSave() {
        seedRecipe("Pancakes", listOf("flour", "milk", "eggs"))

        given()
            .`when`().get("/api/recipes")
            .then()
            .statusCode(200)
            .body("size()", `is`(1))
            .body("[0].title", `is`("Pancakes"))
    }

    @Test
    fun listRespectsLimitParam() {
        repeat(5) { i -> seedRecipe("Recipe $i", listOf("a", "b", "c")) }

        given()
            .queryParam("limit", 3)
            .`when`().get("/api/recipes")
            .then()
            .statusCode(200)
            .body("size()", `is`(3))
    }

    @Test
    fun listRespectsOffsetParam() {
        repeat(5) { i -> seedRecipe("Recipe $i", listOf("a", "b", "c")) }

        given()
            .queryParam("limit", 2)
            .queryParam("offset", 3)
            .`when`().get("/api/recipes")
            .then()
            .statusCode(200)
            .body("size()", `is`(2))
    }

    @Test
    fun getByIdReturnsRecipe() {
        val id = seedRecipe("Omelette", listOf("eggs", "butter", "salt"))

        given()
            .`when`().get("/api/recipes/$id")
            .then()
            .statusCode(200)
            .body("title", `is`("Omelette"))
            .body("id", `is`(id))
            .body("ingredients.size()", `is`(3))
    }

    @Test
    fun listClampsNegativeOffset() {
        seedRecipe("Test", listOf("a", "b", "c"))

        given()
            .queryParam("offset", -5)
            .`when`().get("/api/recipes")
            .then()
            .statusCode(200)
            .body("size()", `is`(1))
    }

    @Test
    fun listClampsExcessiveLimit() {
        repeat(3) { i -> seedRecipe("Recipe $i", listOf("a", "b", "c")) }

        given()
            .queryParam("limit", 999)
            .`when`().get("/api/recipes")
            .then()
            .statusCode(200)
            .body("size()", `is`(3))
    }

    @Test
    fun createReturns201WithLocation() {
        val recipe =
            mapOf(
                "title" to "Test Recipe",
                "ingredients" to listOf("a", "b", "c"),
                "directions" to listOf("step 1", "step 2"),
                "ner" to listOf("a", "b", "c"),
            )

        val location =
            given()
                .contentType("application/json")
                .body(recipe)
                .`when`().post("/api/recipes")
                .then()
                .statusCode(201)
                .extract().header("Location")

        assertNotNull(location)
        assert(location.contains("/api/recipes/"))
    }

    @Test
    fun getByIdReturns404ForUnknown() {
        given()
            .`when`().get("/api/recipes/nonexistent")
            .then()
            .statusCode(404)
    }

    @Test
    fun saveAndRetrieveRecipe() {
        val recipe =
            Recipe(
                title = "Test Soup",
                ingredients = listOf("water", "salt", "pepper"),
                directions = listOf("Boil water", "Add salt"),
                ner = listOf("water", "salt", "pepper"),
            )
        val id = service.save(recipe)

        val found = service.findById(id)
        assertNotNull(found)
        assertEquals("Test Soup", found!!.title)
        assertEquals(3, found.ingredients.size)
    }

    @Test
    fun saveAllAndRetrieve() {
        val recipes =
            listOf(
                Recipe(
                    title = "A",
                    ingredients = listOf("x"),
                    directions = listOf("do x"),
                    ner = listOf("x"),
                ),
                Recipe(
                    title = "B",
                    ingredients = listOf("y"),
                    directions = listOf("do y"),
                    ner = listOf("y"),
                ),
                Recipe(
                    title = "C",
                    ingredients = listOf("z"),
                    directions = listOf("do z"),
                    ner = listOf("z"),
                ),
            )
        service.saveAll(recipes)

        val all = service.findAll(limit = 10)
        assertEquals(3, all.size)
    }

    @Test
    fun saveWithExistingIdUpdates() {
        val id = seedRecipe("Original", listOf("a"))
        val updated =
            Recipe(
                id = id,
                title = "Updated",
                ingredients = listOf("b"),
                directions = listOf("step"),
                ner = listOf("b"),
            )
        service.save(updated)

        val found = service.findById(id)
        assertNotNull(found)
        assertEquals("Updated", found!!.title)
    }

    private fun seedRecipe(
        title: String,
        ingredients: List<String>,
    ): String {
        val doc = firestore.collection("recipes").document()
        doc.set(
            mapOf(
                "title" to title,
                "ingredients" to ingredients,
                "directions" to listOf("Step 1", "Step 2"),
                "ner" to ingredients,
            ),
        ).get()
        return doc.id
    }
}
