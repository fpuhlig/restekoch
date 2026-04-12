package com.restekoch.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.restekoch.embedding.EmbeddingService
import com.restekoch.recipe.Recipe
import com.restekoch.scan.ScanResponse
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@ApplicationScoped
class SemanticCacheService(
    val embeddingService: EmbeddingService,
    val cacheRepository: RedisCacheRepository,
    val objectMapper: ObjectMapper,
    val meterRegistry: MeterRegistry,
    @ConfigProperty(name = "restekoch.cache.enabled", defaultValue = "true")
    val cacheEnabled: Boolean,
    @ConfigProperty(name = "restekoch.cache.similarity-threshold", defaultValue = "0.95")
    val similarityThreshold: Double,
    @ConfigProperty(name = "restekoch.cache.ttl", defaultValue = "3600")
    val ttlSeconds: Long,
) {
    private val log = Logger.getLogger(SemanticCacheService::class.java)

    fun lookup(ingredients: List<String>): ScanResponse? {
        if (!cacheEnabled) return null

        val sample = Timer.start(meterRegistry)
        try {
            val queryVector = embeddingService.embed(ingredients.sorted().joinToString(", "))
            val entry =
                cacheRepository.searchNearest(queryVector, 1.0 - similarityThreshold)
                    ?: return recordMiss(ingredients)
            return buildCachedResponse(entry, ingredients)
        } catch (e: Exception) {
            log.warn("Cache lookup failed, treating as miss: ${e.message}")
            return recordMiss(ingredients)
        } finally {
            sample.stop(meterRegistry.timer("restekoch.cache.lookup.duration"))
        }
    }

    fun store(
        ingredients: List<String>,
        recipes: List<Recipe>,
        explanation: String,
    ) {
        if (!cacheEnabled) return

        try {
            val vector = embeddingService.embed(ingredients.sorted().joinToString(", "))
            val recipesJson = objectMapper.writeValueAsString(recipes)
            val entry =
                CacheEntry(
                    ingredients = ingredients,
                    recipesJson = recipesJson,
                    explanation = explanation,
                )
            cacheRepository.store(vector, entry, ttlSeconds)
            log.info("Cached result for ingredients: $ingredients")
        } catch (e: Exception) {
            log.warn("Cache store failed: ${e.message}")
        }
    }

    fun clear() {
        cacheRepository.clear()
        log.info("Cache cleared")
    }

    fun stats(): CacheStats {
        val hits = meterRegistry.counter("restekoch.cache.hits").count().toLong()
        val misses = meterRegistry.counter("restekoch.cache.misses").count().toLong()
        val entries = cacheRepository.entryCount()
        val total = hits + misses
        val hitRate = if (total > 0) hits.toDouble() / total else 0.0

        return CacheStats(
            entries = entries,
            hits = hits,
            misses = misses,
            hitRate = hitRate,
            enabled = cacheEnabled,
            similarityThreshold = similarityThreshold,
            ttlSeconds = ttlSeconds,
        )
    }

    private fun buildCachedResponse(
        entry: CacheEntry,
        ingredients: List<String>,
    ): ScanResponse {
        meterRegistry.counter("restekoch.cache.hits").increment()
        log.info("Cache hit for ingredients: $ingredients")
        val recipes: List<Recipe> =
            objectMapper.readValue(
                entry.recipesJson,
                objectMapper.typeFactory.constructCollectionType(List::class.java, Recipe::class.java),
            )
        return ScanResponse(
            ingredients = entry.ingredients,
            recipes = recipes,
            explanation = entry.explanation,
            cached = true,
        )
    }

    private fun recordMiss(ingredients: List<String>): Nothing? {
        meterRegistry.counter("restekoch.cache.misses").increment()
        log.info("Cache miss for ingredients: $ingredients")
        return null
    }
}
