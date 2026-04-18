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

        val l1Hit = imageCacheService.lookup(imageBytes)
        if (l1Hit != null) {
            log.info("L1 image cache hit, returning stored response without embedding or search")
            scanTimer.stop(meterRegistry.timer("restekoch.scan.total", "cache", "L1"))
            return l1Hit.copy(cached = true, cacheLevel = "L1")
        }

        val ingredients = geminiService.detectIngredients(imageBytes, mimeType)
        log.info("Detected ${ingredients.size} ingredients: $ingredients")

        val l2Hit = cacheService.lookup(ingredients)
        if (l2Hit != null) {
            log.info("L2 semantic cache hit for ${ingredients.size} ingredients")
            val stored = l2Hit.copy(cached = true, cacheLevel = "L2")
            imageCacheService.store(imageBytes, stored)
            scanTimer.stop(meterRegistry.timer("restekoch.scan.total", "cache", "L2"))
            return stored
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
        val response =
            ScanResponse(
                ingredients = ingredients,
                recipes = recipes,
                explanation = explanation,
                cached = false,
                cacheLevel = null,
            )
        imageCacheService.store(imageBytes, response)
        scanTimer.stop(meterRegistry.timer("restekoch.scan.total", "cache", "miss"))
        return response
    }
}
