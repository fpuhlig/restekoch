package com.restekoch

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class StatusResourceTest {
    @Test
    fun returnsOk() {
        given()
            .`when`().get("/api/status")
            .then()
            .statusCode(200)
            .body("status", `is`("ok"))
    }
}
