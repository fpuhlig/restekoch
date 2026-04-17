package com.restekoch.scan

import com.restekoch.cache.ImageCacheService
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
    val imageCacheService: ImageCacheService,
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

        val imageCacheHit = imageCacheService.lookup(imageBytes)
        val ingredients: List<String>
        val imageHit: Boolean
        if (imageCacheHit != null) {
            log.info("Image cache hit, skipping Gemini Vision")
            ingredients = imageCacheHit
            imageHit = true
        } else {
            ingredients = geminiService.detectIngredients(imageBytes, mimeType)
            log.info("Detected ${ingredients.size} ingredients: $ingredients")
            imageCacheService.store(imageBytes, ingredients)
            imageHit = false
        }

        val cached = cacheService.lookup(ingredients)
        if (cached != null) {
            val level = if (imageHit) "L1+L2" else "L2"
            log.info("Returning cached recipes for ${ingredients.size} ingredients (level $level)")
            scanTimer.stop(meterRegistry.timer("restekoch.scan.total", "cache", "hit"))
            return cached.copy(cached = true, cacheLevel = level)
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
        val tag = if (imageHit) "image" else "miss"
        scanTimer.stop(meterRegistry.timer("restekoch.scan.total", "cache", tag))

        return ScanResponse(
            ingredients = ingredients,
            recipes = recipes,
            explanation = explanation,
            cached = imageHit,
            cacheLevel = if (imageHit) "L1" else null,
        )
    }
}
