package com.restekoch.cache

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
data class CacheEntry(
    val ingredients: List<String>,
    val recipesJson: String,
    val explanation: String,
)
