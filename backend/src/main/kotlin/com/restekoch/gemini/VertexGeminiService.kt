package com.restekoch.gemini

import com.google.cloud.aiplatform.v1.EndpointName
import com.google.cloud.aiplatform.v1.GenerateContentRequest
import com.google.cloud.aiplatform.v1.GenerateContentResponse
import com.google.cloud.aiplatform.v1.GenerationConfig
import com.google.cloud.aiplatform.v1.Part
import com.google.cloud.aiplatform.v1.PredictionServiceClient
import com.google.cloud.aiplatform.v1.PredictionServiceSettings
import com.google.protobuf.ByteString
import io.micrometer.core.annotation.Timed
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@ApplicationScoped
@io.quarkus.arc.profile.IfBuildProfile("prod")
class VertexGeminiService(
    @ConfigProperty(name = "restekoch.gemini.project-id", defaultValue = "restekoch")
    val projectId: String,
    @ConfigProperty(name = "restekoch.gemini.region", defaultValue = "europe-west1")
    val region: String,
    @ConfigProperty(name = "restekoch.gemini.model", defaultValue = "gemini-2.5-flash")
    val model: String,
) : GeminiService {
    private val log = Logger.getLogger(VertexGeminiService::class.java)

    @Timed(value = "restekoch.gemini.detect", description = "Gemini Vision ingredient detection")
    override fun detectIngredients(
        imageBytes: ByteArray,
        mimeType: String,
    ): List<String> {
        val prompt =
            "List all food ingredients visible in this image. " +
                "Return ONLY a comma-separated list of base ingredient names. " +
                "Use the simplest form: 'eggs' not 'hard-boiled eggs', 'tomatoes' not 'cherry tomatoes'. " +
                "No quantities, no preparation methods, no adjectives, no numbering, no explanations."

        val imagePart =
            Part.newBuilder()
                .setInlineData(
                    com.google.cloud.aiplatform.v1.Blob.newBuilder()
                        .setMimeType(mimeType)
                        .setData(ByteString.copyFrom(imageBytes))
                        .build(),
                )
                .build()

        val textPart =
            Part.newBuilder()
                .setText(prompt)
                .build()

        val response = callGemini(listOf(imagePart, textPart))
        val rawText = extractText(response)
        log.info("Gemini detected ingredients: $rawText")

        return rawText
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
    }

    @Timed(value = "restekoch.gemini.explain", description = "Gemini Text recipe explanation")
    override fun explainRecipes(
        ingredients: List<String>,
        recipeTitles: List<String>,
    ): String {
        val prompt =
            "I have these ingredients: ${ingredients.joinToString(", ")}. " +
                "These recipes were suggested: ${recipeTitles.joinToString(", ")}. " +
                "In 2-3 sentences, explain why these recipes are a good match " +
                "for the available ingredients. Be concise."

        val textPart =
            Part.newBuilder()
                .setText(prompt)
                .build()

        val response = callGemini(listOf(textPart))
        return extractText(response)
    }

    private fun callGemini(parts: List<Part>): GenerateContentResponse {
        val endpoint = "$region-aiplatform.googleapis.com:443"
        val settings =
            PredictionServiceSettings.newBuilder()
                .setEndpoint(endpoint)
                .build()

        PredictionServiceClient.create(settings).use { client ->
            val modelName =
                EndpointName.ofProjectLocationPublisherModelName(
                    projectId,
                    region,
                    "google",
                    model,
                )

            val content =
                com.google.cloud.aiplatform.v1.Content.newBuilder()
                    .setRole("user")
                    .addAllParts(parts)
                    .build()

            val generationConfig =
                GenerationConfig.newBuilder()
                    .setTemperature(0.2f)
                    .setMaxOutputTokens(1024)
                    .build()

            val request =
                GenerateContentRequest.newBuilder()
                    .setModel(modelName.toString())
                    .addContents(content)
                    .setGenerationConfig(generationConfig)
                    .build()

            return client.generateContent(request)
        }
    }

    private fun extractText(response: GenerateContentResponse): String {
        return response.candidatesList
            .flatMap { candidate -> candidate.content.partsList }
            .filter { part -> part.hasText() }
            .joinToString("") { part -> part.text }
            .trim()
    }
}
