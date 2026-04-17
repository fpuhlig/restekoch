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
    val imageEntries: Long = 0,
    val imageHits: Long = 0,
    val imageMisses: Long = 0,
    val imageHitRate: Double = 0.0,
    val imageEnabled: Boolean = true,
    val imageTtlSeconds: Long = 0,
)
