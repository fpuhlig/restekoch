package com.restekoch.indexer

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Test

@QuarkusTest
class IndexResourceTest {
    @Test
    fun `POST index returns count`() {
        given()
            .`when`()
            .post("/api/index")
            .then()
            .statusCode(200)
            .body("indexed", greaterThanOrEqualTo(0))
    }
}
