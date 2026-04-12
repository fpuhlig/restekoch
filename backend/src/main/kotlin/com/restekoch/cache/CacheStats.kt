package com.restekoch.cache

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
data class CacheStats(
    val entries: Long,
    val hits: Long,
    val misses: Long,
    val hitRate: Double,
    val enabled: Boolean,
    val similarityThreshold: Double,
    val ttlSeconds: Long,
)
