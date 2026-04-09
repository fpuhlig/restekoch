package com.restekoch.config

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test

@QuarkusTest
class ErrorHandlingTest {
    @Test
    fun `404 returns structured error response`() {
        given()
            .`when`()
            .get("/api/does-not-exist")
            .then()
            .statusCode(404)
            .body("message", notNullValue())
            .body("requestId", notNullValue())
            .body("timestamp", notNullValue())
    }

    @Test
    fun `response contains X-Request-Id header`() {
        given()
            .`when`()
            .get("/api/recipes")
            .then()
            .statusCode(200)
            .header("X-Request-Id", notNullValue())
    }

    @Test
    fun `client-provided request ID is preserved`() {
        given()
            .header("X-Request-Id", "test-request-123")
            .`when`()
            .get("/api/recipes")
            .then()
            .statusCode(200)
            .header("X-Request-Id", equalTo("test-request-123"))
    }

    @Test
    fun `error response contains matching request ID`() {
        given()
            .header("X-Request-Id", "error-test-456")
            .`when`()
            .get("/api/does-not-exist")
            .then()
            .statusCode(404)
            .body("requestId", equalTo("error-test-456"))
            .header("X-Request-Id", equalTo("error-test-456"))
    }
}
