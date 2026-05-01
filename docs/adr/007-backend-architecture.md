# ADR 007: Backend Architecture

## Status

Accepted

## Context

The backend needs a structure for organizing code. Common patterns are MVC, layered architecture (Resource -> Service -> Repository), or direct Resource -> Repository.

## Decision

Three-layer architecture: Resource -> Service -> Repository.

The current structure per feature:
- `Recipe.kt` -- data class, no logic
- `RecipeRepository.kt` -- Firestore access, CRUD operations
- `RecipeService.kt` -- business logic, orchestration between repositories
- `RecipeResource.kt` -- REST endpoints, delegates to service

The service layer exists from the start even when it just forwards to the repository. The RAG pipeline, scan flow, and semantic cache will add real logic to services like `ScanService`, `SearchService`, and `CacheService`. Adding the layer now avoids a refactor later.

## Package structure

```
com.restekoch/
  StatusResource.kt
  config/      (request-id filter, exception mapper, error response)
  recipe/      (Recipe data class, RecipeRepository, RecipeService, RecipeResource)
  scan/        (ScanResource, ScanService, ScanResponse)
  search/      (SearchResource, SearchService, RedisVectorRepository)
  cache/       (CacheResource, CacheStats, ImageCache* and SemanticCache* services + repositories)
  embedding/   (EmbeddingService interface, MockEmbeddingService, VertexEmbeddingService)
  gemini/      (GeminiService interface, MockGeminiService, VertexGeminiService)
  indexer/     (IndexResource, RecipeIndexer)
```

Features are grouped by domain (recipe, scan, search, cache, embedding, gemini, indexer), not by layer. This keeps related code together and each package self-contained.

## Why not full MVC

MVC is a UI pattern. REST APIs do not have views. The "controller" in a REST context is the Resource class. Calling it a controller is technically wrong for a Quarkus REST API.
