# Load Test Findings

Observations collected during load test pilot runs that need follow-up
work. Not results themselves (those live in `load-test-results.md`),
but behavior that deviates from the original design or theory and
should be investigated or improved later.

Each finding has a status: `open`, `under-investigation`, `resolved`,
or `wont-fix`. Each links back to the run that produced it so the
observation is reproducible.

## Findings

### LTF-001: L1-only cache hit takes ~650 ms, dominated by L2 lookup

**Status:** resolved (fixed in ADR 013, branch `feat/l1-full-short-circuit`)
**First observed:** 2026-04-17, Scenario 1 pilot on branch `feat/load-test`
**Category:** performance, architecture note

**Initial expectation:** L1 hit on repeat upload returns in 50 to 200
ms.

**Observed:** p50 637 ms, p95 751 ms across 99 L1 hits. That is ~400
to 500 ms slower than the hypothesis.

**Cause:** Design by layering. L1 caches only ingredient extraction
(the Gemini Vision call). After L1 hit, `ScanService.scan()` still
calls:
1. `SemanticCacheService.lookup(ingredients)` which embeds ingredients
   via Vertex AI text-embedding-004 (~600 to 700 ms) and searches
   Redis for similar past results.
2. On L2 miss: `SearchService.search()` (another embedding call is
   avoided by reusing the query vector) and `GeminiService.explainRecipes()`.

In Scenario 1 the cache was cleared before the run, so all 99 L1 hits
had cold L2. L1 saved the Gemini Vision call (~3 to 5 s each) but the
embedding for L2 lookup still ran on every request.

**Implication:** The expected payoff of L1 is "full pipeline minus
Gemini Vision", not "full pipeline minus everything". Measured benefit
holds: ~13.3 s full scan vs ~650 ms L1 hit = **20x speedup** on the
first repeat, which is the actual user-facing gain.

**If L2 was also warm** (both layers hit): p50 would drop to roughly
the Redis GET latency plus multipart parsing, measured at about 1 s
with cached combined L1+L2 in the manual test on April 16.

**Resolution (2026-04-17, branch `feat/l1-full-short-circuit`):**
On reflection the "design by layering" argument is wrong. L1 is keyed by
the image hash, so two different images never share an L1 entry. The
ingredient-sharing rationale from ADR 012 applies only to L2, not L1.
L1 now stores the full `ScanResponse` (ingredients + recipes +
explanation) as JSON. On L1 hit, `ScanService.scan()` returns the
stored response directly: no embedding call, no L2 lookup, no search,
no Gemini. Full short-circuit. Documented in ADR 013. Expected p50 drop
from ~650 ms to under 100 ms, to be verified by re-running k6
Scenario 1 after deploy.

**Verification (pending):** Re-run k6 Scenario 1 on GCP, update
`docs/load-test-results.md` with before/after numbers.

### LTF-002: Scenario 1 throughput 1.17 req/s

**Status:** resolved (consequence of LTF-001)
**First observed:** 2026-04-17, Scenario 1 pilot
**Category:** performance

**Observed:** 1.17 req/s at 1 VU.

**Cause:** L1 hit takes ~650 ms because of L2 lookup behind it
(see LTF-001). 1 VU at 650 ms per iteration plus 100 ms sleep =
~1.33 iter/s theoretical, 1.17 measured, matches.

**Follow-up:**
- None. Under 10 VU concurrency (Scenario 4) we see 11.96 req/s,
  so horizontal scaling works as expected.

### LTF-003: Scenario 2 search p50 ~740 ms dominated by Vertex AI

**Status:** informational
**First observed:** 2026-04-17, Scenario 2 pilot
**Category:** expected behavior, architecture note

**Observed:** p50 741 ms for GET /api/search with 25 different
ingredient lists. Vertex AI text-embedding-004 is the dominant cost.

**Follow-up:**
- No action needed. This is the external SaaS round-trip cost.
- For the report: note that search latency is SaaS-bound, not
  infrastructure-bound. Scaling the VM will not help; a query-result
  cache would.

### LTF-004: k6 SharedArray does not survive JSON serialization for ArrayBuffer

**Status:** resolved
**First observed:** 2026-04-17, Scenario 1 pilot
**Category:** tooling

**Observed:** Using `new SharedArray('image', () => [open('./f.jpg', 'b')])`
returned `map[string]interface{}` at runtime, and `http.file(image[0], ...)`
crashed with "invalid type, expected string or ArrayBuffer".

**Cause:** k6 SharedArray JSON-serializes values across VUs. An
ArrayBuffer becomes an empty object after the round trip.

**Resolution:** Commit `864dc2c`. Call `open()` at the top level of
the script (init stage). Bind each fixture to a named binding.

**Follow-up:**
- None. Document the pattern in `load-tests/README.md` so future
  scripts do not make the same mistake.

### LTF-005: k6 Prometheus remote write only emits p99 by default

**Status:** resolved
**First observed:** 2026-04-17, Scenario 1 and 2 pilot
**Category:** tooling

**Observed:** Prometheus had `k6_http_req_duration_p99` but no
`_p50`, `_p90`, `_p95`. Grafana time-series panels for load test
latency could not render percentile distributions.

**Cause:** k6 v1 default trend stats for Prometheus remote write is
`p(99)` only. This keeps the Prometheus series count down by default.

**Resolution:** Set
`K6_PROMETHEUS_RW_TREND_STATS="p(50),p(90),p(95),p(99),min,max,avg,med,count"`
as env var before `docker run`.

**Follow-up:**
- Add the env var to `load-tests/README.md` run command examples.
- Consider wrapping the run in a shell helper so the env var is not
  forgotten.

### LTF-006: Grafana panels showed "no data" briefly after pilot runs

**Status:** resolved
**First observed:** 2026-04-17 after the first 3 scenarios ran
**Category:** monitoring

**Observed:** After the first successful scenarios completed, Grafana
panels for Image Cache and Semantic Cache showed "no data" for about
30 seconds, then populated with the expected values.

**Cause:** `instant: true` queries on a `rate()` expression need at
least two Prometheus scrape intervals (15 s default) to produce a
value. Panels evaluated before the second scrape returned empty.

**Resolution:** Wait for 2 to 3 Prometheus scrape intervals before
reading dashboards after a load test starts.

**Follow-up:**
- Optional: switch panel queries to use `increase(...)` with a
  longer range, or set `instant: false` and rely on recent range.
- Low priority, does not block anything.

### LTF-007: fridge4.jpg got deleted during fixture regeneration

**Status:** resolved
**First observed:** 2026-04-17, local fixtures cleanup
**Category:** process bug

**Observed:** During fixture regeneration, fridge4.jpg was listed as
"deleted" by Git and was missing from `load-tests/fixtures/`.

**Cause:** When replacing the single-ingredient images with new
Unsplash downloads, the ordering of cleanup vs rename was incorrect.
fridge4 was moved to `_tmp_fridge4.jpg`, then the final copy to
`fridge4.jpg` was missed.

**Resolution:** Re-downloaded the thermopro pot roast image, resized,
saved to `fridge4.jpg`. Committed as part of
`feat/load-test` branch.

**Follow-up:**
- None. One-off.

### LTF-009: Cache miss count exceeds unique image count under concurrency

**Status:** informational
**First observed:** 2026-04-17, Scenario 4 pilot
**Category:** concurrency, expected behavior

**Expected:** 10 unique images should produce exactly 10 L1 cache
misses (one per first upload), then 1187 L1 hits.

**Observed:** 13 misses total (1184 hits / 1197 iterations). Three
extra misses compared to the theoretical minimum.

**Cause:** Under 10 concurrent VUs, two VUs can upload the same image
before the first one finishes writing the L1 cache entry. Both requests
go through the full Gemini Vision pipeline independently. After both
complete, the second write overwrites the first with the same payload
(deterministic hash), so the cache state is correct but two Gemini
calls were made.

**Impact:** Minor cost overhead in high concurrency. 3 extra Gemini
Vision calls at ~0.003 USD each = 0.009 USD. Not worth fixing for a
student project. For production: use a per-key lock or a background
queue that deduplicates in-flight scans.

**Follow-up:**
- Document in ADR 012 under "Limitations" as known concurrency race.
- Optional future: single-flight pattern (e.g., a ConcurrentHashMap of
  CompletableFuture per hash, return pending future on duplicate).

---

### LTF-008: Scenario 4 setup() cannot call open()

**Status:** resolved
**First observed:** 2026-04-17, Scenario 4 pilot
**Category:** tooling

**Observed:** Scenario 4 setup() function called `open()` for a warm-up
scan. k6 rejects this: "the 'open' function is only available in the
init stage".

**Resolution:** Replaced image-warmup with GET requests against
`/api/cache/stats`. The backend JIT gets exercised, but the cache
stays empty so Scenario 4 measures cold-start L1 misses correctly.

**Follow-up:**
- None. Documented in `load-tests/README.md`.

## Template for new findings

```
### LTF-NNN: short title

**Status:** open / under-investigation / resolved / wont-fix
**First observed:** date, which scenario, which branch or commit
**Category:** performance / tooling / monitoring / security / process

**Expected:** what should happen, ideally with a reference

**Observed:** what happens in reality, with numbers

**Cause:** why, if known

**Resolution:** what fixed it (only when status is resolved)

**Follow-up:** next steps or "none"
```
