package com.restekoch.scan

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@QuarkusTest
class ScanServiceTest {
    @Inject
    lateinit var scanService: ScanService

    @Test
    fun `scan returns detected ingredients`() {
        val result = scanService.scan(ByteArray(100), "image/jpeg", 5)
        assertEquals(listOf("chicken", "tomatoes", "garlic", "olive oil", "basil"), result.ingredients)
    }

    @Test
    fun `scan returns explanation`() {
        val result = scanService.scan(ByteArray(100), "image/jpeg", 5)
        assertTrue(result.explanation.isNotBlank())
    }

    @Test
    fun `scan response contains all fields`() {
        val result = scanService.scan(ByteArray(100), "image/jpeg", 5)
        assertTrue(result.ingredients.isNotEmpty())
        assertTrue(result.explanation.isNotBlank())
    }
}
