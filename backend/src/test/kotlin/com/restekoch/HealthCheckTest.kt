package com.restekoch

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class HealthCheckTest {
    @Test
    fun liveEndpointReturnsUp() {
        given()
            .`when`().get("/q/health/live")
            .then()
            .statusCode(200)
            .body("status", `is`("UP"))
    }

    @Test
    fun readyEndpointReturnsUp() {
        given()
            .`when`().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", `is`("UP"))
    }
}
