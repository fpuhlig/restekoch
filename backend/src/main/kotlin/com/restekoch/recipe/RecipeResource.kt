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
import java.net.URI

@Path("/api/recipes")
@Produces(MediaType.APPLICATION_JSON)
class RecipeResource(
    val service: RecipeService,
) {
    @GET
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
    fun getById(
        @PathParam("id") id: String,
    ): Recipe {
        return service.findById(id)
            ?: throw NotFoundException("Recipe not found")
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun create(recipe: Recipe): Response {
        val id = service.save(recipe)
        return Response.created(URI.create("/api/recipes/$id")).build()
    }
}
