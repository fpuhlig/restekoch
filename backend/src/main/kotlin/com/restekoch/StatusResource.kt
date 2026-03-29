package com.restekoch

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/api/status")
class StatusResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun status(): Map<String, String> = mapOf("status" to "ok")
}
