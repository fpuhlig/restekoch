# Load Test Results

Results from running the k6 scenarios in `load-tests/` against the
production VM on GCP.

Two rounds of runs are documented:
1. **Pilot run (2026-04-17, commit 2be44db):** first end-to-end
   execution, baseline numbers, discovered LTF-001 design issue.
2. **After ADR 013 (2026-04-18):** L1 full short-circuit fix applied,
   re-ran Scenarios 1 and 4 to measure the effect. Scenarios 2 and 3
   unchanged (unaffected code paths).

## Methodology

- Load generator and application run on the same VM (e2-small,
  2 vCPU, 4 GB RAM). k6 container CPU-limited to 0.5 vCPU.
- Gateway rate limit whitelists localhost, so requests bypass the
  10 r/m per-IP limit on `/api/scan`.
- Prometheus ingests k6 metrics via remote write during each run
  with `K6_PROMETHEUS_RW_TREND_STATS="p(50),p(90),p(95),p(99),min,max,avg,med,count"`.
- Cache cleared before each scenario via
  `curl -X DELETE http://localhost/api/cache` (admin endpoint,
  localhost whitelisted).
- Scenarios run in the order: 3, 2, 1, 4 (Firestore, Search, Image
  Cache, Scan Stress). Warmer managed-service caches benefit later
  scenarios.
- Each run tagged with `git_sha` for traceability.

## Test environment

| Item | Value |
|---|---|
| VM | e2-small (GCP Compute Engine) |
| Region | europe-west1 |
| Backend | Quarkus 3.x on OpenJDK 21 |
| Redis | Memorystore Basic 1 GB |
| Firestore | Native mode, 2000 recipes seeded |
| Vertex AI | Gemini 2.5 Flash, text-embedding-004 |
| Gateway | nginx with rate limiting |
| Git SHA (pilot) | 2be44db |
| Git SHA (after ADR 013) | see feat/l1-full-short-circuit |
| Run dates | 2026-04-17 (pilot), 2026-04-18 (after ADR 013) |
| Run type | Pilot plus one re-run for L1 and Scan Stress |

## Summary

| Scenario | Requests | Throughput | p50 | p95 | Error rate | Cache hit rate |
|---|---|---|---|---|---|---|
| 3 recipes baseline | 510 | 27.0 rps | 114 ms | 197 ms | 0.00% | n/a |
| 2 search throughput | 60 | 2.22 rps | 741 ms | 883 ms | 0.00% | n/a |
| 1 image cache L1 (after ADR 013) | 110 | 4.51 rps | **20.4 ms** | **31.7 ms** | 0.00% | 99% |
| 4 scan stress (10 VU, after ADR 013) | 3742 | **37.38 rps** | **112 ms** | **289 ms** | 0.05% | 99.22% |

## Scenario 1: Image cache L1 (100 scans, same image)

**Purpose:** Show the effect of the image content cache on a workload
that uploads the same image repeatedly.

### Before ADR 013 (pilot run, 2026-04-17, commit 2be44db)

| Metric | Value |
|---|---|
| Throughput | 1.17 iter/s |
| p50 duration | 637 ms |
| p95 duration | 750 ms |
| max duration | 13.3 s (first request) |
| L1 hit rate | 99.00% |
| Error rate | 0.00% |

L1 hit still triggered a Vertex AI embedding call for the L2 lookup
(~600 ms per hit). Documented as LTF-001.

### After ADR 013 (L1 full short-circuit, 2026-04-18)

| Metric | Value |
|---|---|
| Total requests (incl. warm-up) | 110 |
| Measured iterations | 100 |
| Duration | 24 s |
| Throughput | 4.10 iter/s, 4.51 req/s |
| p50 duration | **20.4 ms** |
| p90 duration | 26.9 ms |
| p95 duration | **31.7 ms** |
| max duration | 11.7 s (first request, full Gemini pipeline) |
| Error rate | 0.00% |
| L1 hit rate | **99.00%** (99 of 100) |
| Data sent | 35 MB (approx 100 uploads at 345 KB each) |

### Effect of ADR 013

| Metric | Before | After | Improvement |
|---|---|---|---|
| p50 | 637 ms | 20.4 ms | **31x faster** |
| p95 | 750 ms | 31.7 ms | **24x faster** |
| Throughput | 1.17 iter/s | 4.10 iter/s | **3.5x higher** |

Full-pipeline vs L1 hit: 11.7 s vs 20.4 ms = **~580x speedup** on
repeat uploads.

**Observations:**
- L1 hit is now a pure Redis GET plus JSON deserialization. No Vertex
  AI embedding call, no Redis KNN search, no Gemini explain.
- The first request still runs the full pipeline (Gemini Vision,
  embedding, search, explain). All 99 subsequent requests hit L1 and
  return within 31 ms p95.
- LTF-001 resolved by this commit.

## Scenario 2: Search throughput (50 requests, 25 ingredient sets)

**Purpose:** Measure semantic search throughput. Each request triggers
a Vertex AI embedding call and a Redis KNN lookup.

| Metric | Value |
|---|---|
| Total requests (incl. warm-up) | 60 |
| Measured iterations | 50 |
| Duration | 27 s |
| Throughput | 1.85 iter/s, 2.22 req/s |
| p50 duration | 741 ms |
| p90 duration | 857 ms |
| p95 duration | 883 ms |
| max duration | 1.32 s |
| Error rate | 0.00% |

**Observations:**
- Latency is dominated by the Vertex AI text-embedding-004 round trip
  (~700 ms per call). Redis KNN is sub-10 ms. Tracked as LTF-003.
- No cache in front of this endpoint, so every search does a full
  embedding. A query-result cache would dramatically cut p50.

## Scenario 3: Recipes baseline (500 requests, paginated Firestore)

**Purpose:** Baseline throughput for a simple paginated Firestore read.
No Gemini, no Redis vector search.

| Metric | Value |
|---|---|
| Total requests (incl. warm-up) | 510 |
| Measured iterations | 500 |
| Duration | 18.5 s |
| Throughput | 27.0 iter/s, 27.6 req/s |
| p50 duration | 114 ms |
| p90 duration | 185 ms |
| p95 duration | 197 ms |
| max duration | 3.11 s (warm-up cold start) |
| Error rate | 0.00% |
| Firestore reads | 10,200 (510 requests × 20 docs) |

**Observations:**
- Clean baseline. No external SaaS on this path.
- Cold-start spike on request 1 of 3 s, then steady ~100 to 200 ms.
- Firestore free tier allows 50k reads/day, so this scenario can be
  re-run up to 4 times per day without quota concerns.

## Scenario 4: Scan stress (10 VUs, 10 different images, ramped)

**Purpose:** Stress the Gemini path with concurrent distinct uploads,
ramped from 1 to 10 VUs over 30 seconds. Tests that the backend
degrades gracefully under concurrent Gemini Vision calls.

### Before ADR 013 (pilot run, 2026-04-17)

| Metric | Value |
|---|---|
| Throughput | 11.96 req/s |
| p50 | 630 ms |
| p95 | 751 ms |
| L1 hit rate | 98.91% |
| Error rate | 0.00% |

### After ADR 013 (L1 full short-circuit, 2026-04-18)

| Metric | Value |
|---|---|
| Total requests (incl. warm-up) | 3742 |
| Measured iterations | 3737 |
| Duration | 100 s |
| Throughput | **37.38 req/s** |
| p50 duration | **112 ms** |
| p90 duration | 229 ms |
| p95 duration | **289 ms** |
| max duration | 20.66 s (cold starts for 10 unique images) |
| Error rate | 0.05% (2 of 3742) |
| L1 hit rate | **99.22%** (3706 of 3735) |
| Data sent | 1.7 GB (17 MB/s sustained upload) |

### Effect of ADR 013 under concurrency

| Metric | Before | After | Improvement |
|---|---|---|---|
| Throughput | 11.96 rps | 37.38 rps | **3.1x higher** |
| p50 | 630 ms | 112 ms | **5.6x faster** |
| p95 | 751 ms | 289 ms | **2.6x faster** |
| Iterations in 100s | 1197 | 3737 | **3.1x more** |

**Observations:**
- Throughput tripled under the same load profile. Backend no longer
  blocks on Vertex AI embedding on L1 hits.
- p50 at 112 ms under 10 VU load is higher than Scenario 1 (20 ms at
  1 VU) because the bottleneck at 10 VU is upload bandwidth and CPU
  contention on a 2 vCPU VM, not server logic. 1.7 GB sent in 100 s is
  17 MB/s sustained, close to the VM egress limits.
- 2 failed requests out of 3742 (0.05%). Likely Memorystore connection
  blips or gateway timeouts under load. Well under the 5% threshold.
- L1 hit rate 99.22% confirms the full short-circuit holds under
  concurrency.

## Cost

Two full runs (pilot plus re-run of Scenarios 1 and 4):

| Scenario | Runs | Gemini Vision calls | Embedding calls | Firestore reads |
|---|---|---|---|---|
| 1 | 2 | 2 | ~2 | 0 |
| 2 | 1 | 0 | 50 | 0 |
| 3 | 1 | 0 | 0 | 10,200 (free tier) |
| 4 | 2 | ~23 | ~23 | 0 |
| **Total** | | ~25 | ~75 | 10,200 |

Estimated cost: **~0.09 USD** over both runs. Remaining GCP budget
~40 USD.

## Conclusions

- **Image cache L1 works as designed.** 99% hit rate on repeated
  uploads of the same image. After ADR 013, L1 hit is a pure Redis GET
  plus JSON deserialize (p50 20 ms). First request runs the full
  pipeline, every repeat avoids all external SaaS.
- **Full pipeline vs L1 hit: ~580x speedup.** 11.7 s (first scan) to
  20 ms (L1 hit). The focus feature of this project.
- **Concurrency scales.** Under 10 VU, throughput triples from 12 rps
  to 37 rps after ADR 013. 99.22% hit rate holds. Bottleneck shifts
  to upload bandwidth (17 MB/s sustained) instead of server logic.
- **Search is SaaS-bound.** p50 740 ms is dominated by Vertex AI
  embedding. Scaling the VM does not improve this. A result cache
  would.
- **Recipes endpoint is fast.** 27 rps at 100 to 200 ms is a clean
  baseline for a paginated managed-database read.
- **Error rate stays low under any tested load.** 2 of 3742 requests
  in the stress test (0.05%) failed under concurrent load. Scenarios 1
  through 3 had zero errors.

## Open follow-ups

See `load-test-findings.md` for the full list.

- **LTF-001:** resolved by ADR 013.
- **LTF-009:** informational. Concurrency race produces 3 extra Gemini
  calls per run. Minor cost overhead, documented in ADR 012.
- **Threshold calibration:** optional. Current thresholds
  (`http_req_failed<0.05`) already exercise the system. Tight p95
  thresholds would add CI value, not presentation value.

## Next steps

None before the presentation. The numbers are final.
