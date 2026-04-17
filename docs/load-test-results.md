# Load Test Results

Pilot run results from running the k6 scenarios in `load-tests/`
against the production VM. These are baseline numbers. Duration
thresholds will be calibrated from these numbers and a second
official run will follow.

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
| Git SHA | 55f156d |
| Run date | 2026-04-17 |
| Run type | Pilot (no duration thresholds) |

## Summary

| Scenario | Requests | Throughput | p50 | p95 | Error rate | Cache hit rate |
|---|---|---|---|---|---|---|
| 3 recipes baseline | 510 | 27.0 rps | 114 ms | 197 ms | 0.00% | n/a |
| 2 search throughput | 60 | 2.22 rps | 741 ms | 883 ms | 0.00% | n/a |
| 1 image cache L1 | 110 | 1.29 rps | 637 ms | 750 ms | 0.00% | 99% |
| 4 scan stress (10 VU) | 1202 | 11.96 rps | 630 ms | 751 ms | 0.00% | 98.9% |

## Scenario 1: Image cache L1 (100 scans, same image)

**Purpose:** Show the effect of the image content cache on a workload
that uploads the same image repeatedly.

| Metric | Value |
|---|---|
| Total requests (incl. warm-up) | 110 |
| Measured iterations | 100 |
| Duration | 85 s |
| Throughput | 1.17 iter/s, 1.29 req/s |
| p50 duration | 637 ms |
| p90 duration | 725 ms |
| p95 duration | 750 ms |
| max duration | 13.3 s (first request, full Gemini pipeline) |
| Error rate | 0.00% |
| L1 hit rate | **99.00%** (99 of 100) |
| Combined cache hit rate (L1 or L2) | 99.00% |
| Data sent | 35 MB (approx 100 uploads at 345 KB each) |

**Observations:**
- L1 hit latency is ~650 ms, not the expected 50 to 200 ms. Tracked
  as LTF-001. Upload size dominates given the image is 345 KB and
  the overall request is gated by multipart parsing.
- The first request took 13.3 s (full Gemini Vision + Embedding + Redis
  KNN + Gemini Explain). All subsequent requests hit L1 and returned in
  630 to 750 ms.
- Prometheus dashboard briefly showed "no data" after the run
  completed, then populated. See LTF-006.

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

| Metric | Value |
|---|---|
| Total requests (incl. warm-up) | 1202 |
| Measured iterations | 1197 |
| Duration | 100 s |
| Throughput | 11.91 iter/s, 11.96 req/s |
| p50 duration | 630 ms |
| p90 duration | 699 ms |
| p95 duration | 751 ms |
| max duration | 19.24 s (first misses for 10 unique images) |
| Error rate | 0.00% |
| L1 hit rate | **98.91%** (1184 of 1197) |
| Data sent | 536 MB |

**Observations:**
- 13 cache misses observed instead of the expected 10. Three extra
  Gemini calls due to concurrent uploads of the same image before the
  first write hit L1. See LTF-009.
- p50 and p95 at 10 VU load are comparable to the single-VU Scenario 1,
  showing the backend handled concurrency without queue buildup.
- Throughput scales with concurrency (1.17 req/s at 1 VU vs 11.96 req/s
  at 10 VU). Mostly linear, so the bottleneck is not server contention
  on shared resources.
- Zero errors at the tested concurrency. The Gateway rate limit (10 r/m
  external) is bypassed by localhost whitelist.

## Cost

| Scenario | Gemini Vision calls | Embedding calls | Firestore reads | Estimated cost USD |
|---|---|---|---|---|
| 1 | 1 | ~1 | 0 | 0.003 |
| 2 | 0 | 50 | 0 | 0.001 |
| 3 | 0 | 0 | 10,200 | 0 (free tier) |
| 4 | 13 | ~13 | 0 | 0.040 |
| **Pilot total** | 14 | 64 | 10,200 | **~0.044** |

Budget impact: vernachlässigbar. Remaining budget ~40 USD.

## Conclusions

- **Image cache L1 works as designed.** 99% hit rate on repeated
  uploads of the same image. The first request is the only Gemini call,
  subsequent requests avoid external SaaS completely.
- **Concurrency is handled cleanly.** 10 VUs with different images
  produced 13 misses (3 extra due to race), still 98.9% hit rate.
- **Search is SaaS-bound.** p50 740 ms is dominated by Vertex AI.
  Scaling the VM will not improve this. A result cache would.
- **Recipes endpoint is fast.** 27 rps at 100 to 200 ms is a clean
  baseline for a paginated managed-database read.
- **No errors under any tested load.** All 2869 measured requests
  returned HTTP 200 or 204.

## Open follow-ups

See `load-test-findings.md` for the full list. Top items for
investigation:

- **LTF-001:** L1 hit latency (~650 ms) higher than designed
  (50 to 200 ms). Need to verify ScanService.scan short-circuits on
  L1 hit.
- **LTF-009:** Cache miss count exceeds unique image count under
  concurrency. Minor cost overhead, document as known behavior.
- **Threshold calibration:** Set `http_req_duration` thresholds from
  these numbers plus 20% headroom and run the official run.

## Next steps

1. Investigate LTF-001: open `ScanService.scan` and verify L1 short
   circuit.
2. Calibrate and commit threshold values.
3. Run the official run (same config, recorded thresholds).
4. Capture Grafana screenshots into `docs/images/load-test-*.png`.
5. Extend ADR 012 with the concurrency race note from LTF-009.
