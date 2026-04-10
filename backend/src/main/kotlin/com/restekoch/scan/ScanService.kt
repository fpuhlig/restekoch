package com.restekoch.scan

import com.restekoch.gemini.GeminiService
import com.restekoch.search.SearchService
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

@ApplicationScoped
class ScanService(
    val geminiService: GeminiService,
    val searchService: SearchService,
) {
    private val log = Logger.getLogger(ScanService::class.java)

    fun scan(
        imageBytes: ByteArray,
        mimeType: String,
        limit: Int = 5,
    ): ScanResponse {
        log.info("Scanning image: ${imageBytes.size} bytes, $mimeType")

        val ingredients = geminiService.detectIngredients(imageBytes, mimeType)
        log.info("Detected ${ingredients.size} ingredients: $ingredients")

        val recipes = searchService.search(ingredients, limit)
        log.info("Found ${recipes.size} matching recipes")

        val explanation =
            if (recipes.isNotEmpty()) {
                geminiService.explainRecipes(ingredients, recipes.map { it.title })
            } else {
                "No matching recipes found for the detected ingredients."
            }

        return ScanResponse(
            ingredients = ingredients,
            recipes = recipes,
            explanation = explanation,
        )
    }
}
