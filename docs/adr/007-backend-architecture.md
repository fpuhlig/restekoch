# ADR 007: Backend Architecture

Status: accepted

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
  recipe/
    Recipe.kt
    RecipeRepository.kt
    RecipeService.kt
    RecipeResource.kt
  scan/        (coming)
  search/      (coming)
  cache/       (coming)
```

Features are grouped by domain (recipe, scan, search), not by layer. This keeps related code together and each package self-contained.

## Why not full MVC

MVC is a UI pattern. REST APIs do not have views. The "controller" in a REST context is the Resource class. Calling it a controller is technically wrong for a Quarkus REST API.
