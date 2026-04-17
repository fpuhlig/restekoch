# Load Tests

Four k6 scenarios that exercise the Restekoch stack on the production VM.
Designed to run from inside the VM (localhost is whitelisted by the
nginx rate limit). Results stream into Prometheus via remote write and
show up on the Grafana dashboard in real time.

## Scenarios

| # | Script | Purpose | Requests | Gemini calls |
|---|---|---|---|---|
| 1 | `01-image-cache-l1.js` | Image cache L1 hit rate with repeated uploads of fridge1.jpg | 100 | 1 |
| 2 | `02-search-throughput.js` | `/api/search` throughput with 25 different ingredient lists | 50 | 0 |
| 3 | `03-recipes-baseline.js` | `/api/recipes` paginated read baseline | 500 | 0 |
| 4 | `04-scan-stress.js` | 10 concurrent VUs with 10 different images, ramped | ~50-100 | ~10 |

Total Gemini cost per full run: ~USD 0.03 (Gemini Flash 2.5 Vision at
about USD 0.003 per image).

## Prerequisites

- VM is up and the stack is healthy (`docker compose ps` all `Up` and
  `healthy`).
- Prometheus was (re-)deployed with
  `--web.enable-remote-write-receiver` enabled. Check with
  `curl http://localhost:9090/api/v1/status/flags | jq '.data | keys'`.
  The flag must be present.
- Grafana Dashboard "Restekoch" is open in a browser for manual
  screenshots during and after each run.
- Local clone of the repo on the VM at a stable path, on the feature or
  main branch that contains these scripts.

## Pre-flight checklist (run on the VM)

```bash
df -h                                       # enough disk for Prometheus
timedatectl                                 # NTP synced
docker ps                                   # all services running
curl -sS http://localhost/health            # gateway health
curl -sS http://localhost/api/cache/stats | jq  # cache state snapshot
curl -sS -X DELETE http://localhost/api/cache    # clean cache (admin, localhost whitelisted)
curl -sS http://localhost/api/cache/init -X POST # recreate indexes
```

## Running a scenario

Order matters because earlier scenarios warm managed service caches that
later scenarios benefit from. Recommended order: 3, 2, 1, 4.

```bash
cd /path/to/restekoch/load-tests

# Clean cache before each scenario so results are deterministic.
curl -sS -X DELETE http://localhost/api/cache

# Run the scenario in a CPU-limited k6 container.
docker run --rm -i --network host --cpus 0.5 \
  -v "$(pwd):/work" -w /work grafana/k6:latest \
  run \
  --out experimental-prometheus-rw=http://localhost:9090/api/v1/write \
  --tag git_sha="$(git -C .. rev-parse --short HEAD)" \
  03-recipes-baseline.js
```

Repeat with `02-search-throughput.js`, `01-image-cache-l1.js`,
`04-scan-stress.js` in that order, clearing the cache between each.

## Thresholds calibration

Initial scripts have only error-rate thresholds. Duration thresholds
must be calibrated from a pilot run. Process:

1. Run each scenario once without duration thresholds. Record p50, p95,
   p99 from the k6 summary.
2. Set `http_req_duration` thresholds to `p(95)<X` where X is the
   observed p95 plus 20% headroom.
3. Commit the updated thresholds.
4. Run again. This is the official run. Summary goes into
   `docs/load-test-results.md`, screenshots go into `docs/images/`.

## Results artifacts

- `docs/load-test-results.md`: human-readable summary per scenario with
  p50/p95/p99, throughput, error rate, observations, git SHA.
- `docs/images/load-test-{scenario}-{metric}.png`: Grafana screenshots
  captured during each run.
- k6 JSON output is not checked in. Prometheus is the source of truth
  for time-series data.

## Known limitations

- **Same-host topology.** Load generator and application run on the
  same VM. Network latency is near zero, and the k6 container shares
  CPU with the backend even with `--cpus 0.5`. The numbers are useful
  for relative comparison (cache-on vs cache-off, scenario vs
  scenario), not for absolute production capacity planning. A separate
  load-gen VM would fix this but was out of scope for this project.
- **Firestore free tier cap.** Scenario 3 consumes ~10,000 reads per
  run (500 requests times 20 docs). Free tier is 50,000 reads per day.
  Limit full test runs to 4 per day.
- **Gemini rate limits.** Vertex AI Gemini Flash default is 60 RPM per
  project per region. Scenario 4 ramps to 10 concurrent VUs over 30
  seconds and stays for 60 seconds, well under the limit.
