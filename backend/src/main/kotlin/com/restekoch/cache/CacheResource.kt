package com.restekoch.cache

import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@Path("/api/cache")
@Tag(name = "Cache")
@Produces(MediaType.APPLICATION_JSON)
class CacheResource(
    val cacheService: SemanticCacheService,
    val imageCacheService: ImageCacheService,
) {
    @GET
    @Path("/stats")
    @Operation(summary = "Get cache statistics for semantic and image caches")
    fun stats(): CacheStats {
        val semantic = cacheService.stats()
        val image = imageCacheService.stats()
        return CacheStats(
            entries = semantic.entries,
            hits = semantic.hits,
            misses = semantic.misses,
            hitRate = semantic.hitRate,
            enabled = semantic.enabled,
            similarityThreshold = semantic.similarityThreshold,
            ttlSeconds = semantic.ttlSeconds,
            imageEntries = image.entries,
            imageHits = image.hits,
            imageMisses = image.misses,
            imageHitRate = image.hitRate,
            imageEnabled = image.enabled,
            imageTtlSeconds = image.ttlSeconds,
        )
    }

    @DELETE
    @Operation(summary = "Clear both semantic and image caches")
    fun clear(): Response {
        cacheService.clear()
        imageCacheService.clear()
        return Response.noContent().build()
    }

    @POST
    @Path("/init")
    @Operation(summary = "Initialize the cache indexes")
    fun init(): Response {
        cacheService.clear()
        imageCacheService.clear()
        return Response.ok(mapOf("status" to "initialized")).build()
    }
}
