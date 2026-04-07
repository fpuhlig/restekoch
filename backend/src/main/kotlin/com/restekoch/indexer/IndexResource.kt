package com.restekoch.indexer

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/api/index")
@Produces(MediaType.APPLICATION_JSON)
class IndexResource(
    val recipeIndexer: RecipeIndexer,
) {
    @POST
    fun index(): Map<String, Any> {
        val count = recipeIndexer.indexAll()
        return mapOf("indexed" to count)
    }
}
