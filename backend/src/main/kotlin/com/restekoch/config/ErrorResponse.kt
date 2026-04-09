package com.restekoch.config

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
data class ErrorResponse(
    val message: String,
    val requestId: String,
    val timestamp: String,
)
