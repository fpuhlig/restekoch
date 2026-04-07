# ADR 008: Embedding Model and Vector Search Architecture

Status: accepted

## Context

The app needs to find recipes that match a given set of ingredients. A simple text search (SQL LIKE or keyword matching) misses semantic relationships: "butter" would not match "margarine", "pasta" would not match "noodles". Vector search solves this by comparing mathematical representations of meaning.

We need to choose an embedding model and a vector search backend.

## Decision

**Embedding model: text-embedding-004 (Vertex AI)**

768 dimensions. Sufficient for ingredient-level semantic matching. Cheaper and faster than gemini-embedding-001 (3072 dimensions). If text-embedding-004 gets deprecated during the project lifetime, gemini-embedding-001 with output_dimensionality=768 is a drop-in replacement.

Task types: RETRIEVAL_DOCUMENT for indexing recipe ingredients, RETRIEVAL_QUERY for search queries.

**Vector search: Redis with RediSearch (HNSW index)**

Redis Stack includes RediSearch which supports vector fields with HNSW (Hierarchical Navigable Small World) approximate nearest neighbor search. HNSW is fast and accurate enough for 2000 recipes. FLAT (exact search) would also work at this scale but HNSW is the standard choice and scales better.

We already have Memorystore Redis 7.2 provisioned on GCP (ADR 003). Memorystore supports vector search with the same FT.CREATE / FT.SEARCH commands as Redis Stack. Locally we use Redis Stack via Quarkus DevService.

**Vector dimensions: 768, distance metric: cosine similarity**

Cosine similarity measures the angle between vectors, ignoring magnitude. Two ingredient lists with similar items point in similar directions regardless of how many items each list has.

## Alternatives Considered

- gemini-embedding-001: 3072 dimensions, better quality, but 4x more memory per vector and higher API cost. Overkill for matching ingredient names.
- Vertex AI Vector Search (managed service): separate GCP product, adds cost and complexity. Redis already handles our scale.
- pgvector (PostgreSQL): would require adding PostgreSQL to the stack. Redis is already in the architecture.

## Consequences

- Each recipe needs one Vertex AI API call during indexing (one-time cost)
- Each search query needs one Vertex AI API call for the query embedding
- Redis stores 2000 vectors * 768 dimensions * 4 bytes = ~6 MB. Fits easily in 1 GB Memorystore
- Tests use MockEmbeddingService (deterministic fake vectors) to avoid API calls in CI
