# ADR 009: Gemini Model and RAG Pipeline

## Status

Accepted (April 2026), with a verification note added on 2026-04-30 — see *Verification note* below.

> **Verification note (2026-04-30):** During an audit pass before submission, parts of the model survey under *Decision &rarr; Model* could not be substantiated against primary sources. Specifically, the comparative claims about "Gemini 3.1 Pro", "Gemini 3.1 Flash" and "Gemini 3 Flash" (preview status, percentage advantages, the CalCam reference, and the Arxiv accuracy figure) lack citations and may not match the actual Vertex AI model lineup at the time of writing. The substantive choice for `gemini-2.5-flash` rests on (a) its GA status, (b) its lower price per token compared to the corresponding Pro variant in the same family, and (c) the existing Vertex AI integration. Those three points carry the decision; the wider model survey is retained below as the original draft argument and should be read with this caveat.

## Context

Restekoch needs two AI capabilities: image analysis (detect ingredients from a fridge photo) and text generation (explain why recipes match). Both use Google's Gemini models via Vertex AI.

Key decisions: which Gemini model, which SDK, and how the RAG pipeline connects the components.

## Decision

### Model: gemini-2.5-flash

Available options as of April 2026:
- Gemini 3.1 Pro: $2/$12 per 1M tokens. Best reasoning. Overkill for ingredient detection.
- Gemini 3.1 Flash: Preview status. Better benchmarks but no stability guarantee. API may change.
- Gemini 3 Flash: Preview status. 15% better than 2.5 Flash on complex extraction tasks (handwriting, financial documents). Minimal difference for food recognition.
- Gemini 2.5 Flash: GA (General Availability). $0.15/$0.60 per 1M tokens. Stable API with SLA.
- Gemini 2.0 Flash: Deprecated.

We chose gemini-2.5-flash because:
1. GA status means stable API. Presentation is April 20, we cannot risk a Preview API breaking.
2. Food ingredient recognition is not a complex reasoning task. Flash models handle it well. Google's own CalCam app uses Flash for food recognition.
3. Cheapest option at $0.15 per 1M input tokens. Budget is ~$50 student credits.
4. Arxiv paper (November 2025) shows Gemini achieves 9.2/10 factual accuracy on food image recognition.

The model name is a config property (`restekoch.gemini.model`). Switching to a newer model requires changing one line, no code changes.

### SDK: google-cloud-aiplatform (existing dependency)

Two options:
- `google-cloud-aiplatform`: Already in the project for embeddings. Uses `PredictionServiceClient` with gRPC. More verbose API.
- `google-genai` (v1.47.0): Newer SDK with simpler API (`Client.builder().vertexAI(true)`). Would add a new dependency.

We stay with `google-cloud-aiplatform` to avoid adding a second GCP AI dependency. The embedding code already uses this library. More verbose but no version conflicts.

### RAG Pipeline

The scan endpoint orchestrates these steps:

1. Client uploads a photo (POST /api/scan, multipart)
2. Gemini Vision analyzes the image, returns a comma-separated ingredient list
3. EmbeddingService embeds the ingredient list (text-embedding-004, exists from Phase 4)
4. RedisVectorRepository runs KNN search against indexed recipes (exists from Phase 4)
5. RecipeRepository loads full recipe data from Firestore (exists from Phase 2)
6. Gemini Text explains why each recipe matches the detected ingredients
7. Response contains: detected ingredients, matching recipes, explanations

Steps 3-5 reuse existing components. Steps 1-2 and 6-7 are new.

### Mock Strategy

Same pattern as EmbeddingService:
- `GeminiService` interface with `detectIngredients()` and `explainRecipes()`
- `VertexGeminiService` with `@IfBuildProfile("prod")` for real Vertex AI calls
- `MockGeminiService` with `@DefaultBean` returning fixed responses for dev/test

## Consequences

- Ingredient detection quality depends on Gemini 2.5 Flash. Configurable model name allows quick switch if needed.
- No new dependencies added.
- Pipeline reuses 3 existing components (EmbeddingService, RedisVectorRepository, RecipeRepository).
- Mock strategy enables full test coverage without GCP credentials.
- Must test on GCP before merging. Local mock does not validate real Gemini responses.
