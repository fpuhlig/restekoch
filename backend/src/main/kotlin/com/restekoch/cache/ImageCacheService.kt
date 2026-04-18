package com.restekoch.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.restekoch.scan.ScanResponse
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.security.MessageDigest

@ApplicationScoped
class ImageCacheService(
    val cacheRepository: ImageCacheRepository,
    val objectMapper: ObjectMapper,
    val meterRegistry: MeterRegistry,
    @ConfigProperty(name = "restekoch.image-cache.enabled", defaultValue = "true")
    val cacheEnabled: Boolean,
    @ConfigProperty(name = "restekoch.image-cache.ttl", defaultValue = "86400")
    val ttlSeconds: Long,
    @ConfigProperty(name = "restekoch.gemini.model", defaultValue = "gemini-2.5-flash")
    val modelName: String,
) {
    private val log = Logger.getLogger(ImageCacheService::class.java)

    fun hash(bytes: ByteArray): String {
        require(bytes.isNotEmpty()) { "Image bytes must not be empty" }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun keyFor(hash: String): String = "${ImageCacheRepository.PREFIX}$modelName:$hash"

    fun lookup(imageBytes: ByteArray): ScanResponse? {
        if (!cacheEnabled) return null

        val sample = Timer.start(meterRegistry)
        try {
            val key = keyFor(hash(imageBytes))
            val raw = cacheRepository.get(key)
            if (raw == null) {
                meterRegistry.counter("restekoch.image_cache.misses").increment()
                log.debug("Image cache miss for key $key")
                return null
            }
            val response = objectMapper.readValue(raw, ScanResponse::class.java)
            meterRegistry.counter("restekoch.image_cache.hits").increment()
            log.info("Image cache hit for key $key")
            return response
        } catch (e: Exception) {
            log.warn("Image cache lookup failed, treating as miss: ${e.message}")
            meterRegistry.counter("restekoch.image_cache.misses").increment()
            return null
        } finally {
            sample.stop(meterRegistry.timer("restekoch.image_cache.lookup.duration"))
        }
    }

    fun store(
        imageBytes: ByteArray,
        response: ScanResponse,
    ) {
        if (!cacheEnabled) return
        if (response.ingredients.isEmpty()) return

        try {
            val key = keyFor(hash(imageBytes))
            val json = objectMapper.writeValueAsString(response)
            cacheRepository.store(key, json, ttlSeconds)
            log.info("Stored image cache entry for key $key")
        } catch (e: Exception) {
            log.warn("Image cache store failed: ${e.message}")
        }
    }

    fun clear() {
        cacheRepository.clear()
        log.info("Image cache cleared")
    }

    fun stats(): ImageCacheStats {
        val hits = meterRegistry.counter("restekoch.image_cache.hits").count().toLong()
        val misses = meterRegistry.counter("restekoch.image_cache.misses").count().toLong()
        val entries = cacheRepository.entryCount()
        val total = hits + misses
        val hitRate = if (total > 0) hits.toDouble() / total else 0.0
        return ImageCacheStats(
            entries = entries,
            hits = hits,
            misses = misses,
            hitRate = hitRate,
            enabled = cacheEnabled,
            ttlSeconds = ttlSeconds,
        )
    }
}

@RegisterForReflection
data class ImageCacheStats(
    val entries: Long,
    val hits: Long,
    val misses: Long,
    val hitRate: Double,
    val enabled: Boolean,
    val ttlSeconds: Long,
)
