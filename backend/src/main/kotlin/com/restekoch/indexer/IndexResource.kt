package com.restekoch.indexer

import com.restekoch.cache.SemanticCacheService
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.logging.Logger

@Path("/api/index")
@Tag(name = "Index")
@Produces(MediaType.APPLICATION_JSON)
class IndexResource(
    val recipeIndexer: RecipeIndexer,
    val cacheService: SemanticCacheService,
) {
    private val log = Logger.getLogger(IndexResource::class.java)

    @POST
    @Operation(summary = "Rebuild recipe index and clear cache")
    fun index(): Map<String, Any> {
        val count = recipeIndexer.indexAll()
        cacheService.clear()
        log.info("Re-indexed $count recipes, cache cleared")
        return mapOf("indexed" to count)
    }
}
