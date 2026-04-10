package com.restekoch.scan

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test
import java.io.File

@QuarkusTest
class ScanResourceTest {
    private fun createTestImage(): File {
        val file = File.createTempFile("test-image", ".jpg")
        file.deleteOnExit()
        file.writeBytes(ByteArray(100) { it.toByte() })
        return file
    }

    @Test
    fun `scan with image returns ingredients and recipes`() {
        given()
            .multiPart("image", createTestImage(), "image/jpeg")
            .`when`()
            .post("/api/scan")
            .then()
            .statusCode(200)
            .body("ingredients", notNullValue())
            .body("ingredients", hasItems("chicken", "tomatoes"))
            .body("explanation", notNullValue())
    }

    @Test
    fun `scan without image returns 400`() {
        given()
            .contentType("multipart/form-data")
            .`when`()
            .post("/api/scan")
            .then()
            .statusCode(400)
    }

    @Test
    fun `scan with non-image file returns 400`() {
        val textFile = File.createTempFile("test", ".txt")
        textFile.deleteOnExit()
        textFile.writeText("not an image")

        given()
            .multiPart("image", textFile, "text/plain")
            .`when`()
            .post("/api/scan")
            .then()
            .statusCode(400)
    }

    @Test
    fun `scan with empty image returns 400`() {
        val emptyFile = File.createTempFile("empty", ".jpg")
        emptyFile.deleteOnExit()
        emptyFile.writeBytes(ByteArray(0))

        given()
            .multiPart("image", emptyFile, "image/jpeg")
            .`when`()
            .post("/api/scan")
            .then()
            .statusCode(400)
    }

    @Test
    fun `scan respects limit parameter`() {
        given()
            .multiPart("image", createTestImage(), "image/jpeg")
            .queryParam("limit", 3)
            .`when`()
            .post("/api/scan")
            .then()
            .statusCode(200)
            .body("ingredients", notNullValue())
    }
}
