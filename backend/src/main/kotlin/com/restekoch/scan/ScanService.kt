package com.restekoch.scan

import com.restekoch.cache.SemanticCacheService
import com.restekoch.gemini.GeminiService
import com.restekoch.search.SearchService
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

@ApplicationScoped
class ScanService(
    val geminiService: GeminiService,
    val searchService: SearchService,
    val cacheService: SemanticCacheService,
    val meterRegistry: MeterRegistry,
) {
    private val log = Logger.getLogger(ScanService::class.java)

    fun scan(
        imageBytes: ByteArray,
        mimeType: String,
        limit: Int = 5,
    ): ScanResponse {
        val scanTimer = Timer.start(meterRegistry)
        log.info("Scanning image: ${imageBytes.size} bytes, $mimeType")

        val ingredients = geminiService.detectIngredients(imageBytes, mimeType)
        log.info("Detected ${ingredients.size} ingredients: $ingredients")

        val cached = cacheService.lookup(ingredients)
        if (cached != null) {
            log.info("Returning cached result for ${ingredients.size} ingredients")
            scanTimer.stop(meterRegistry.timer("restekoch.scan.total", "cache", "hit"))
            return cached
        }

        val recipes = searchService.search(ingredients, limit)
        log.info("Found ${recipes.size} matching recipes")

        val explanation =
            if (recipes.isNotEmpty()) {
                geminiService.explainRecipes(ingredients, recipes.map { it.title })
            } else {
                "No matching recipes found for the detected ingredients."
            }

        cacheService.store(ingredients, recipes, explanation)
        scanTimer.stop(meterRegistry.timer("restekoch.scan.total", "cache", "miss"))

        return ScanResponse(
            ingredients = ingredients,
            recipes = recipes,
            explanation = explanation,
            cached = false,
        )
    }
}
