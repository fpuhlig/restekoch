package com.restekoch.cache

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyScanArgs
import io.quarkus.redis.datasource.value.SetArgs
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
open class ImageCacheRepository(
    val redis: RedisDataSource,
) {
    companion object {
        const val PREFIX = "img:"
    }

    open fun get(key: String): String? {
        return redis.value(String::class.java).get(key)
    }

    open fun store(
        key: String,
        value: String,
        ttlSeconds: Long,
    ) {
        redis.value(String::class.java).set(key, value, SetArgs().ex(ttlSeconds))
    }

    open fun entryCount(): Long {
        var count = 0L
        for (key in redis.key().scan(KeyScanArgs().match("$PREFIX*").count(100)).toIterable()) {
            count++
        }
        return count
    }

    open fun clear() {
        val keysApi = redis.key()
        val toDelete = mutableListOf<String>()
        for (key in keysApi.scan(KeyScanArgs().match("$PREFIX*").count(100)).toIterable()) {
            toDelete.add(key)
        }
        if (toDelete.isNotEmpty()) {
            keysApi.del(*toDelete.toTypedArray())
        }
    }
}
