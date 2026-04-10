package com.restekoch.scan

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.multipart.FileUpload

@Path("/api/scan")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Scan", description = "Fridge photo scanning and recipe suggestion via RAG pipeline")
class ScanResource(
    val scanService: ScanService,
) {
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
        summary = "Scan a fridge photo for ingredients and suggest recipes",
        description =
            "Uploads an image, detects ingredients via Gemini Vision, " +
                "searches matching recipes via vector similarity, and explains the matches",
    )
    fun scan(
        @RestForm("image") image: FileUpload?,
        @QueryParam("limit") @DefaultValue("5") limit: Int,
    ): ScanResponse {
        if (image == null) {
            throw IllegalArgumentException("Image file is required")
        }

        val mimeType = image.contentType() ?: "image/jpeg"
        if (!mimeType.startsWith("image/")) {
            throw IllegalArgumentException("File must be an image (got $mimeType)")
        }

        val maxSize = 10 * 1024 * 1024
        val imageBytes = image.uploadedFile().toFile().readBytes()
        if (imageBytes.isEmpty()) {
            throw IllegalArgumentException("Image file is empty")
        }
        if (imageBytes.size > maxSize) {
            throw IllegalArgumentException("Image exceeds 10 MB limit")
        }

        return scanService.scan(imageBytes, mimeType, limit.coerceIn(1, 20))
    }
}
