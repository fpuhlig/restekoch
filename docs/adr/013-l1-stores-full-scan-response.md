# ADR 013: L1 Image Cache Stores Full Scan Response

## Status

Accepted. Supersedes in part ADR 012 (storage format only).

## Context

ADR 012 introduced the L1 image content cache as a SHA-256 keyed lookup that stored only the list of ingredients detected by Gemini Vision. The design rationale was that two different images producing the same ingredient list could share a single downstream L2 semantic cache entry.

Phase 11 load testing on 2026-04-17 revealed that this rationale is flawed. L1 is keyed by the hash of the image bytes. Two different images produce different hashes by definition. No sharing is possible at the L1 layer. The sharing argument applies only to L2 (which is keyed by the ingredient embedding).

The practical consequence is that every L1 hit in `ScanService.scan()` still calls `SemanticCacheService.lookup(ingredients)`, which runs a Vertex AI embedding round-trip (~600 ms) before the Redis KNN search. Measured in Scenario 1 of the pilot load test: p50 637 ms at L1 hit, instead of the target 50 to 200 ms. Documented as LTF-001 in `docs/load-test-findings.md`.

## Decision

L1 stores the full `ScanResponse` as a JSON string, not just the ingredients list.

On L1 hit, `ScanService.scan()` deserializes the stored response, sets `cached=true` and `cacheLevel="L1"`, and returns immediately. It does not call the embedding service, does not touch L2, does not call the search service, and does not call Gemini Explain.

On L1 miss, the full pipeline runs as before: Gemini Vision, L2 lookup, optional search and explain. The single L1 write happens at the end of the miss path with the complete response, so cache state stays consistent.

Backward compatibility: old L1 entries stored under ADR 012 (ingredient list JSON) will fail deserialization against the new `ScanResponse` type. The existing try-catch in `ImageCacheService.lookup()` treats deserialization errors as cache misses, so old entries silently fall through to the full pipeline and get overwritten with the new format on the first miss. No manual cache clear required after deploy. Stale entries expire within the 24 hour TTL.

## Consequences

Positive:

- L1 hit becomes a pure Redis GET plus JSON deserialization. Measured on GCP after deploy: p50 20.4 ms, p95 31.7 ms (k6 Scenario 1, 100 iterations, 99 L1 hits).
- Speedup from full miss to L1 hit rises from 20x (13.3 s to 650 ms) to ~580x measured (11.7 s first scan to 20 ms repeat).
- L1 fulfils its original intent: the cheapest, fastest layer that short-circuits everything downstream.
- Presentation impact: the focus feature now has a harder, more defensible number.

Negative:

- Redis memory per L1 entry rises from roughly 100 bytes (ingredient list JSON) to roughly 2 to 5 KB (full response JSON with recipes and explanation). At 10 000 unique images, total L1 size is about 50 MB, well under the 1 GB Memorystore Basic instance.
- Stale ingredient-only L1 entries from ADR 012 format are effectively dead until the 24 hour TTL expires. Acceptable for this project.
- L2 no longer benefits from L1 hits at all. L2 exists only for cross-image similarity (different image, similar ingredients). This is the correct semantic boundary. L2 usage numbers in the Grafana dashboard will drop to match this new role. Dashboard panels remain valid.

Neutral:

- No new dependencies.
- No new configuration properties.
- No change to Ansible, Terraform, CI, or docker-compose.
- Prometheus metrics unchanged for the image cache counters. `restekoch.image_cache.hits`, `restekoch.image_cache.misses`, `restekoch.image_cache.lookup.duration` all keep their meaning.
- `restekoch.scan.total` timer tag values change. Before: `cache=hit`, `cache=miss`, `cache=image`. After: `cache=L1`, `cache=L2`, `cache=miss`. The Grafana dashboard panel "Avg scan latency by cache state" was updated to match (L1 Hit and L2 Hit series instead of a single Hit series).

## Alternatives considered

1. **Keep ADR 012, cache the embedding vector alongside the ingredients.** Rejected: still requires the vector for L2 KNN search, so we save the Vertex AI call but still run the Redis vector search. Incomplete short-circuit, more moving parts.
2. **Redis Hash with separate fields per response element.** Rejected: SemanticCacheService already uses JSON strings via Jackson. Keeping the same pattern reduces cognitive load and code duplication.
3. **Add a second cache layer between L1 and L2 for the "L1 hit with precomputed L2 result" case.** Rejected: unnecessary complexity. The full-response L1 covers this directly.

## Follow-up

- k6 Scenarios 1 and 4 re-run on GCP on 2026-04-18 after deploy. Before/after numbers in `docs/load-test-results.md`.
- LTF-001 resolved in this branch.
- Numbers ready for presentation: L1 hit p50 20 ms, full-miss-to-L1-hit speedup ~580x, 10 VU concurrency throughput 37 req/s (3x higher than before).
