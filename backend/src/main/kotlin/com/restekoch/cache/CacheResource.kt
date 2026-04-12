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
) {
    @GET
    @Path("/stats")
    @Operation(summary = "Get cache statistics")
    fun stats(): CacheStats {
        return cacheService.stats()
    }

    @DELETE
    @Operation(summary = "Clear the semantic cache")
    fun clear(): Response {
        cacheService.clear()
        return Response.noContent().build()
    }

    @POST
    @Path("/init")
    @Operation(summary = "Initialize the cache index")
    fun init(): Response {
        cacheService.clear()
        return Response.ok(mapOf("status" to "initialized")).build()
    }
}
