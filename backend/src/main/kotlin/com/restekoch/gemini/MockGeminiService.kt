package com.restekoch.gemini

import io.quarkus.arc.DefaultBean
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

@ApplicationScoped
@DefaultBean
class MockGeminiService : GeminiService {
    private val log = Logger.getLogger(MockGeminiService::class.java)

    override fun detectIngredients(
        imageBytes: ByteArray,
        mimeType: String,
    ): List<String> {
        log.info("Mock ingredient detection for ${imageBytes.size} bytes ($mimeType)")
        return listOf("chicken", "tomatoes", "garlic", "olive oil", "basil")
    }

    override fun explainRecipes(
        ingredients: List<String>,
        recipeTitles: List<String>,
    ): String {
        val ingredientText = ingredients.joinToString(", ")
        return recipeTitles.joinToString(" ") { title ->
            "$title works well with $ingredientText."
        }
    }
}
