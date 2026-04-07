package com.restekoch.embedding

import com.google.cloud.aiplatform.v1.EndpointName
import com.google.cloud.aiplatform.v1.PredictRequest
import com.google.cloud.aiplatform.v1.PredictionServiceClient
import com.google.protobuf.Value
import com.google.protobuf.util.JsonFormat
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
@io.quarkus.arc.profile.IfBuildProfile("prod")
class VertexEmbeddingService(
    @ConfigProperty(name = "restekoch.embedding.project-id", defaultValue = "restekoch")
    val projectId: String,
    @ConfigProperty(name = "restekoch.embedding.region", defaultValue = "europe-west1")
    val region: String,
    @ConfigProperty(name = "restekoch.embedding.model", defaultValue = "text-embedding-004")
    val model: String,
) : EmbeddingService {
    override fun embed(text: String): FloatArray {
        return embedBatch(listOf(text))[0]
    }

    override fun embedBatch(texts: List<String>): List<FloatArray> {
        val endpoint =
            EndpointName.ofProjectLocationPublisherModelName(
                projectId,
                region,
                "google",
                model,
            )

        PredictionServiceClient.create().use { client ->
            val instances =
                texts.map { text ->
                    val builder = Value.newBuilder()
                    JsonFormat.parser().merge("""{"content": "$text"}""", builder)
                    builder.build()
                }

            val request =
                PredictRequest.newBuilder()
                    .setEndpoint(endpoint.toString())
                    .addAllInstances(instances)
                    .build()

            val response = client.predict(request)

            return response.predictionsList.map { prediction ->
                val values =
                    prediction.structValue
                        .fieldsMap["embeddings"]!!
                        .structValue
                        .fieldsMap["values"]!!
                        .listValue
                        .valuesList
                FloatArray(values.size) { i -> values[i].numberValue.toFloat() }
            }
        }
    }
}
