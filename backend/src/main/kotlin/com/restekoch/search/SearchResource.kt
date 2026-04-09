package com.restekoch.search

import com.restekoch.recipe.Recipe
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@Path("/api/search")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Search", description = "Semantic recipe search via vector similarity")
class SearchResource(
    val searchService: SearchService,
) {
    @GET
    @Operation(
        summary = "Search recipes by ingredients",
        description = "Embeds the query and finds similar recipes via KNN vector search",
    )
    fun search(
        @QueryParam("ingredients") ingredients: String?,
        @QueryParam("limit") @DefaultValue("10") limit: Int,
    ): List<Recipe> {
        if (ingredients.isNullOrBlank()) {
            return emptyList()
        }

        val ingredientList =
            ingredients
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

        if (ingredientList.isEmpty()) {
            return emptyList()
        }

        return searchService.search(ingredientList, limit.coerceIn(1, 50))
    }
}
