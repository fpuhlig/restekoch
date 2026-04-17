# 012: Image Content Cache (L1)

## Status

Accepted

## Context

The semantic ingredient cache (ADR 010) caches recipe suggestions keyed by the ingredient embedding. Every scan still runs a Gemini Vision call to extract ingredients from the image, even if the exact same image was uploaded minutes earlier.

Three problems follow from always calling Gemini:

1. Cost. Gemini Vision bills per request. Repeat uploads of the same image burn budget with no new information.
2. Latency. Gemini Vision adds roughly 8 seconds to the request path. A repeat scan feels slow.
3. Future feature. A planned enhancement lets users send the same image with an added text modifier (for example "vegan only"). Without a content cache, each modifier re-runs Vision on identical bytes.

Gemini also offers Context Caching and Implicit Caching, but neither fits our pattern. Context Caching requires a 4096 token minimum and targets large documents or videos. Implicit Caching needs shared prefixes across requests, which our image uploads do not have.

## Decision

Add a second cache layer (L1) in front of the existing semantic cache (L2). The scan flow becomes:

```
Scan Request
  -> L1 hash cache (this ADR): img:{model}:{sha256}
       HIT:  reuse ingredients, skip Gemini Vision
       MISS: call Gemini, store L1
  -> L2 semantic cache (ADR 010): embedding vector
       HIT:  return full cached response
       MISS: vector search, Firestore lookup, Gemini explain, store L2
  -> Response
```

Implementation details:

- Hash algorithm: SHA-256 via `java.security.MessageDigest`. No new dependency.
- Key format: `img:{model-name}:{hex-hash}`. The model name is part of the key so switching Gemini versions invalidates old entries automatically.
- Value: JSON array of ingredient strings.
- Storage: Redis string key-value via `quarkus-redis-client` `value(...)` API with SETEX TTL. No RediSearch needed because lookup is exact match.
- TTL: 86400 seconds (24 hours). Image bytes are immutable, so longer caching is safe. Semantic L2 keeps 3600s because recipe rankings can evolve.
- Metrics: Micrometer counters `restekoch.image_cache.hits` and `restekoch.image_cache.misses`, timer `restekoch.image_cache.lookup.duration`.
- Response flag: `cacheLevel` field with values `"L1"`, `"L2"`, `"L1+L2"`, or `null`. Non-breaking addition alongside existing `cached` boolean.
- Failure mode: any cache exception is logged and treated as a miss. Cache must never break the scan path.
- Server-side hashing. The client uploads the image, the backend hashes it. Client-side hashing was considered but rejected for now (see Limitations below).

## Consequences

Positive:

- Repeat scans of the same file skip Gemini Vision entirely. Cost saving of roughly $0.002 per hit plus a latency drop from ~10s to ~2.8s.
- The L1 cache is simpler than L2: exact key lookup, no embedding, no vector search. Latency on hit is below 10ms.
- Model name in the key prevents stale results after a model upgrade.
- Two independent layers. L1 stores ingredients, L2 stores recipes keyed by ingredient embedding. No overlap, no coupling.

Negative:

- Identical bytes required. Two photos of the same fridge from slightly different angles produce different hashes and both miss L1. Only re-uploads of the same file benefit.
- One more component in the scan pipeline. More tests, more metrics, more dashboard panels.
- Memory pressure on large uploads. The hash needs the full byte array. Max request size is 12 MB, backend heap is 256 MB, so safe, but worth noting.
- L1 poisoning risk on a failed Gemini response. Mitigated by storing L1 only after a successful Gemini call.

## Limitations and future work

- **No HTTPS, so no client-side hashing.** The Web Crypto API that enables SHA-256 in the browser is restricted to secure contexts. Our current deployment uses HTTP on the VM public IP. Client-side hashing with a preflight check would save the 12 MB upload on every cache hit, but the feature cannot ship until HTTPS is in place.
- **Wrong Gemini detections are cached for 24 hours.** If Gemini misclassifies an ingredient (for example detecting chicken instead of steak), the wrong result is returned on every subsequent scan of the same image until the TTL expires. There is currently no user-facing way to override or invalidate a single entry. Workaround during demo: upload a re-taken photo from a different angle, which produces a different hash and bypasses L1.
- Future enhancement: preflight `POST /api/scan/check` with the hash in the body. Server responds with the cached ingredients on hit or 204 on miss. Only on miss does the client upload the image.
- Future enhancement: domain with Let's Encrypt certificate for HTTPS.
- Future enhancement: force rescan via `?force=true` query parameter that bypasses both cache layers and overwrites the stored entry.
- Future enhancement: manual ingredient correction in the frontend so the user can remove or add tags and request recipes for the corrected list.
- Future enhancement: image modifier feature (same image plus text prompt). Requires L1 hit + modifier-aware L2 lookup, or a third cache layer keyed on `(imageHash, modifier)`.

## Alternatives considered

- **Gemini Context Caching**. Rejected. Minimum 4096 tokens, targets large documents. Our prompt is a single image plus a short instruction.
- **Gemini Implicit Caching**. Rejected. Requires shared prefix between requests. Each image is different, so no prefix benefit.
- **Quarkus Cache Extension with Caffeine**. Rejected. Already using Redis for L2 semantic cache, and Quarkus Cache Extension does not integrate with our existing SemanticCacheService pattern. Custom Redis calls keep both cache layers consistent.
- **Perceptual hashing (pHash, dHash)**. Rejected. Perceptual hashes would let near-duplicate images share a cache entry, useful in principle for repeated fridge photos. But they require an image processing dependency (ImageIO, OpenCV bindings, or a Kotlin library), and their false-positive risk at low Hamming distance is poorly bounded for food photography. SHA-256 is exact and predictable.
- **Client-side hashing with preflight**. Deferred. See Limitations.
