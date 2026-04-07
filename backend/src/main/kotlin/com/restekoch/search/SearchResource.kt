package com.restekoch.search

import com.restekoch.recipe.Recipe
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Path("/api/search")
@Produces(MediaType.APPLICATION_JSON)
class SearchResource(
    val searchService: SearchService,
) {
    @GET
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
