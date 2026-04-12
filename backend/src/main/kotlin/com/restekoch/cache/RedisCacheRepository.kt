package com.restekoch.cache

import com.restekoch.embedding.EmbeddingService
import io.quarkus.redis.datasource.RedisDataSource
import io.vertx.mutiny.redis.client.Command
import io.vertx.mutiny.redis.client.Redis
import io.vertx.mutiny.redis.client.Request
import io.vertx.mutiny.redis.client.Response
import jakarta.enterprise.context.ApplicationScoped
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@ApplicationScoped
open class RedisCacheRepository(
    val redis: RedisDataSource,
    val redisClient: Redis,
) {
    companion object {
        const val INDEX_NAME = "idx:cache"
        const val PREFIX = "cache:"
        const val VECTOR_FIELD = "embedding"
        const val INGREDIENTS_FIELD = "ingredients"
        const val RECIPES_FIELD = "recipes_json"
        const val EXPLANATION_FIELD = "explanation"
    }

    open fun createIndex() {
        try {
            redis.execute("FT.DROPINDEX", INDEX_NAME)
        } catch (_: Exception) {
            // index does not exist, that is fine
        }
        redis.execute(
            "FT.CREATE", INDEX_NAME,
            "ON", "HASH",
            "PREFIX", "1", PREFIX,
            "SCHEMA",
            VECTOR_FIELD, "VECTOR", "HNSW", "6",
            "TYPE", "FLOAT32",
            "DIM", EmbeddingService.VECTOR_DIM.toString(),
            "DISTANCE_METRIC", "COSINE",
            INGREDIENTS_FIELD, "TAG",
        )
    }

    open fun store(
        vector: FloatArray,
        entry: CacheEntry,
        ttlSeconds: Long,
    ) {
        val key = "$PREFIX${UUID.randomUUID()}"
        val vectorBytes = floatArrayToBytes(vector)

        val request =
            Request.cmd(Command.HSET)
                .arg(key)
                .arg(VECTOR_FIELD).arg(vectorBytes)
                .arg(INGREDIENTS_FIELD).arg(entry.ingredients.joinToString(","))
                .arg(RECIPES_FIELD).arg(entry.recipesJson)
                .arg(EXPLANATION_FIELD).arg(entry.explanation)

        redisClient.send(request).await().indefinitely()

        val expireRequest =
            Request.cmd(Command.EXPIRE)
                .arg(key)
                .arg(ttlSeconds)
        redisClient.send(expireRequest).await().indefinitely()
    }

    open fun searchNearest(
        queryVector: FloatArray,
        distanceThreshold: Double,
    ): CacheEntry? {
        val vectorBytes = floatArrayToBytes(queryVector)

        val request =
            Request.cmd(Command.create("FT.SEARCH"))
                .arg(INDEX_NAME)
                .arg("*=>[KNN 1 @$VECTOR_FIELD \$BLOB]")
                .arg("PARAMS").arg(2)
                .arg("BLOB").arg(vectorBytes)
                .arg("DIALECT").arg(2)

        val response = redisClient.send(request).await().indefinitely()
        return parseCacheResult(response, distanceThreshold)
    }

    open fun clear() {
        // drop index (also removes indexed hashes if created with DD option, but we use DROPINDEX only)
        try {
            redis.execute("FT.DROPINDEX", INDEX_NAME)
        } catch (_: Exception) {
            // index does not exist
        }
        // delete all cache keys via SCAN to avoid blocking KEYS on large datasets
        try {
            var cursor = "0"
            do {
                val result = redis.execute("SCAN", cursor, "MATCH", "$PREFIX*", "COUNT", "100")
                cursor = result.get(0).toString()
                val keys = result.get(1)
                for (i in 0 until keys.size()) {
                    redis.execute("DEL", keys.get(i).toString())
                }
            } while (cursor != "0")
        } catch (_: Exception) {
            // no keys to delete
        }
        // recreate empty index
        createIndex()
    }

    open fun entryCount(): Long {
        return try {
            val info = redis.execute("FT.INFO", INDEX_NAME)
            val infoStr = info.toString()
            val numDocsMatch = Regex("num_docs[=:]?(\\d+)").find(infoStr)
            numDocsMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun parseCacheResult(
        response: Response,
        distanceThreshold: Double,
    ): CacheEntry? {
        // try RESP3 map format first, then RESP2 array
        return parseCacheResultMap(response, distanceThreshold)
            ?: parseCacheResultArray(response, distanceThreshold)
    }

    private fun parseCacheResultMap(
        response: Response,
        distanceThreshold: Double,
    ): CacheEntry? {
        try {
            val resultsNode = response.get("results") ?: return null
            if (resultsNode.size() == 0) return null

            val entry = resultsNode.get(0)
            val attrs = entry.get("extra_attributes")

            val scoreKey = "__${VECTOR_FIELD}_score"
            val score = attrs.get(scoreKey).toString().toDoubleOrNull() ?: return null
            if (score > distanceThreshold) return null

            val ingredients = attrs.get(INGREDIENTS_FIELD).toString().split(",")
            val recipesJson = attrs.get(RECIPES_FIELD).toString()
            val explanation = attrs.get(EXPLANATION_FIELD).toString()

            return CacheEntry(
                ingredients = ingredients,
                recipesJson = recipesJson,
                explanation = explanation,
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseCacheResultArray(
        response: Response,
        distanceThreshold: Double,
    ): CacheEntry? {
        try {
            // RESP2: [count, docId, [field, value, field, value, ...]]
            if (response.size() < 3) return null
            val count = response.get(0).toInteger()
            if (count == 0) return null

            val fields = response.get(2)
            val fieldMap = mutableMapOf<String, String>()
            var i = 0
            while (i < fields.size() - 1) {
                val key = fields.get(i).toString()
                val value = fields.get(i + 1).toString()
                fieldMap[key] = value
                i += 2
            }

            val scoreKey = "__${VECTOR_FIELD}_score"
            val score = fieldMap[scoreKey]?.toDoubleOrNull() ?: return null
            if (score > distanceThreshold) return null

            return CacheEntry(
                ingredients = (fieldMap[INGREDIENTS_FIELD] ?: "").split(","),
                recipesJson = fieldMap[RECIPES_FIELD] ?: "[]",
                explanation = fieldMap[EXPLANATION_FIELD] ?: "",
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun floatArrayToBytes(vector: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        vector.forEach { buffer.putFloat(it) }
        return buffer.array()
    }
}
