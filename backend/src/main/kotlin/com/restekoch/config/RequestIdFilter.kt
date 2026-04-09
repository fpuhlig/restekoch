package com.restekoch.config

import jakarta.inject.Inject
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.MDC
import java.util.UUID

@Provider
class RequestIdFilter : ContainerRequestFilter, ContainerResponseFilter {
    companion object {
        const val HEADER = "X-Request-Id"
        const val MDC_KEY = "requestId"
    }

    @Inject
    lateinit var requestIdHolder: RequestIdHolder

    override fun filter(request: ContainerRequestContext) {
        val incoming = request.getHeaderString(HEADER)
        val requestId = if (incoming.isNullOrBlank()) UUID.randomUUID().toString() else incoming
        requestIdHolder.id = requestId
        MDC.put(MDC_KEY, requestId)
    }

    override fun filter(
        request: ContainerRequestContext,
        response: ContainerResponseContext,
    ) {
        val existing = response.getHeaderString(HEADER)
        if (existing.isNullOrEmpty()) {
            response.headers.putSingle(HEADER, requestIdHolder.id)
        }
        MDC.remove(MDC_KEY)
    }
}
