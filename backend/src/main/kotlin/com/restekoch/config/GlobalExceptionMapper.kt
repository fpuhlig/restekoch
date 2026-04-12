package com.restekoch.config

import io.vertx.core.http.HttpServerRequest
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import java.time.Instant

class GlobalExceptionMapper {
    private val log = Logger.getLogger(GlobalExceptionMapper::class.java)

    @Inject
    lateinit var requestIdHolder: RequestIdHolder

    @Context
    lateinit var httpRequest: HttpServerRequest

    @ServerExceptionMapper
    fun mapNotFound(e: NotFoundException): Response {
        return buildResponse(Response.Status.NOT_FOUND, e.message ?: "Resource not found")
    }

    @ServerExceptionMapper
    fun mapIllegalArgument(e: IllegalArgumentException): Response {
        return buildResponse(Response.Status.BAD_REQUEST, e.message ?: "Invalid request")
    }

    @ServerExceptionMapper
    fun mapGeneric(e: Exception): Response {
        log.error("Unhandled exception", e)
        return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error")
    }

    private fun buildResponse(
        status: Response.Status,
        message: String,
    ): Response {
        val requestId =
            requestIdHolder.id.ifEmpty {
                httpRequest.getHeader(RequestIdFilter.HEADER) ?: ""
            }
        return Response.status(status)
            .entity(
                ErrorResponse(
                    message = message,
                    requestId = requestId,
                    timestamp = Instant.now().toString(),
                ),
            )
            .header(RequestIdFilter.HEADER, requestId)
            .build()
    }
}
