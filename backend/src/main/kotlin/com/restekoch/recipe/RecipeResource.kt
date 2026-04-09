package com.restekoch.recipe

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.net.URI

@Path("/api/recipes")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Recipes", description = "Recipe CRUD operations")
class RecipeResource(
    val service: RecipeService,
) {
    @GET
    @Operation(summary = "List recipes", description = "Returns a paginated list of recipes")
    fun list(
        @QueryParam("limit") @DefaultValue("20") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int,
    ): List<Recipe> =
        service.findAll(
            limit = limit.coerceIn(1, 100),
            offset = maxOf(0, offset),
        )

    @GET
    @Path("/{id}")
    @Operation(summary = "Get recipe by ID", description = "Returns a single recipe by its document ID")
    fun getById(
        @PathParam("id") id: String,
    ): Recipe {
        return service.findById(id)
            ?: throw NotFoundException("Recipe not found")
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create recipe", description = "Stores a new recipe in Firestore")
    fun create(recipe: Recipe): Response {
        val id = service.save(recipe)
        return Response.created(URI.create("/api/recipes/$id")).build()
    }
}
