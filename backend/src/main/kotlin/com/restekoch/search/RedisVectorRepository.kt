package com.restekoch.search

import com.restekoch.embedding.EmbeddingService
import io.quarkus.redis.datasource.RedisDataSource
import io.vertx.mutiny.redis.client.Command
import io.vertx.mutiny.redis.client.Redis
import io.vertx.mutiny.redis.client.Request
import io.vertx.mutiny.redis.client.Response
import jakarta.enterprise.context.ApplicationScoped
import java.nio.ByteBuffer
import java.nio.ByteOrder

@ApplicationScoped
class RedisVectorRepository(
    val redis: RedisDataSource,
    val redisClient: Redis,
) {
    companion object {
        const val INDEX_NAME = "idx:recipes"
        const val PREFIX = "recipe:"
        const val VECTOR_FIELD = "embedding"
        const val NER_FIELD = "ner"
    }

    fun createIndex() {
        // drop existing index first (idempotent, ignores error if index does not exist)
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
        )
    }

    fun storeRecipeVector(
        recipeId: String,
        ner: String,
        vector: FloatArray,
    ) {
        val key = "$PREFIX$recipeId"
        val vectorBytes = floatArrayToBytes(vector)

        val request =
            Request.cmd(Command.HSET)
                .arg(key)
                .arg(NER_FIELD).arg(ner)
                .arg(VECTOR_FIELD).arg(vectorBytes)

        redisClient.send(request).await().indefinitely()
    }

    fun searchSimilar(
        queryVector: FloatArray,
        limit: Int = 10,
    ): List<String> {
        val vectorBytes = floatArrayToBytes(queryVector)

        val request =
            Request.cmd(Command.create("FT.SEARCH"))
                .arg(INDEX_NAME)
                .arg("*=>[KNN $limit @$VECTOR_FIELD \$BLOB]")
                .arg("PARAMS").arg(2)
                .arg("BLOB").arg(vectorBytes)
                .arg("DIALECT").arg(2)

        val response = redisClient.send(request).await().indefinitely()
        return parseSearchResults(response)
    }

    private fun floatArrayToBytes(vector: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        vector.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    private fun parseSearchResults(response: Response): List<String> {
        val results = mutableListOf<String>()
        try {
            val resultsNode = response.get("results") ?: return results
            for (j in 0 until resultsNode.size()) {
                val entry = resultsNode.get(j)
                val docId = entry.get("id").toString()
                results.add(docId.removePrefix(PREFIX))
            }
        } catch (e: Exception) {
            // fallback for array-style response
            if (response.size() > 1) {
                var i = 1
                while (i < response.size()) {
                    results.add(response.get(i).toString().removePrefix(PREFIX))
                    i += 2
                }
            }
        }
        return results
    }
}
