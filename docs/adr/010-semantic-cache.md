# ADR 010: Semantic Cache Design

## Status

Accepted

## Context

The scan endpoint runs a full RAG pipeline on every request: embed ingredients, Redis KNN search, Firestore reads, Gemini text explanation. This takes about 2 seconds per request even when the same or similar ingredients were queried before. For the university project, semantic caching is the registered focus feature.

## Decision

A separate Redis vector index (idx:cache) stores previous scan results keyed by the ingredient embedding vector. Before running the RAG pipeline, the system checks if a cached result exists within a configurable cosine similarity threshold.

Cache sits between Gemini Vision (ingredient detection) and the RAG pipeline. Gemini Vision runs on every request because each photo is different. The cache saves the expensive part: recipe search and Gemini text explanation.

Threshold is 0.95 cosine similarity (0.05 distance). This is strict by design. A lower threshold (0.80-0.85) would increase hit rate but risk returning wrong recipes for different ingredient sets. For a cooking app, precision matters more than hit rate. The threshold is a config property and can be adjusted without code changes.

TTL is 1 hour. Cache entries expire because recipes could be re-indexed and cached results would become stale. The recipe indexer also clears the cache on re-index.

## Why not an existing cache library

Redis is already in the stack for vector search. Adding a separate cache library (Quarkus Cache, Caffeine) would mean a second caching layer that does not support semantic similarity. Building on the existing RedisVectorRepository pattern keeps the approach consistent.

## Metrics

Three Micrometer metrics for the Grafana dashboard:
- restekoch.cache.hits (counter)
- restekoch.cache.misses (counter)
- restekoch.cache.lookup.duration (timer)

## Consequences

The first request for any ingredient combination is still slow (full RAG pipeline). Subsequent requests with similar ingredients return from cache. The cache index uses the same Memorystore Redis instance as the recipe index, sharing the 1GB memory budget. Each cache entry is roughly 5KB (768 floats + JSON), so thousands of entries fit comfortably.

Memorystore has the same field type limitations as for the recipe index: only VECTOR and TAG fields in FT.CREATE. The cache stores recipe JSON and explanation text as plain HASH fields outside the index schema.
