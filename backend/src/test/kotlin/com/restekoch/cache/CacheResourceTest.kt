package com.restekoch.cache

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Test

@QuarkusTest
class CacheResourceTest {
    @Test
    fun `get cache stats returns valid response`() {
        given()
            .`when`()
            .get("/api/cache/stats")
            .then()
            .statusCode(200)
            .body("enabled", equalTo(true))
            .body("hits", greaterThanOrEqualTo(0))
            .body("misses", greaterThanOrEqualTo(0))
    }

    @Test
    fun `delete cache returns 204`() {
        given()
            .`when`()
            .delete("/api/cache")
            .then()
            .statusCode(204)
    }

    @Test
    fun `init cache returns 200`() {
        given()
            .`when`()
            .post("/api/cache/init")
            .then()
            .statusCode(200)
            .body("status", equalTo("initialized"))
    }
}
