# Restekoch

Scan your fridge, get recipe ideas.

Upload a photo of your ingredients. The app identifies them and suggests matching recipes using RAG and semantic caching.

## Architecture

```
Client (Browser)
  |
  v
Gateway (nginx, port 80)
  |  rate limiting on /api/scan (10 req/min per IP)
  |  admin restriction on /api/index and /api/cache (internal only)
  |  security headers (X-Content-Type-Options, X-Frame-Options, Referrer-Policy)
  |
  +-- /           -> Frontend (static files)
  +-- /api/*      -> Backend
  +-- /health     -> 200 ok (gateway health)
  |
  v
Frontend (React + Vite)
  |  photo upload, ingredient display, recipe cards
  v
Backend (Kotlin + Quarkus, port 8080)
  |-- /api/status       -> health check
  |-- /api/recipes      -> CRUD (list, get by ID, create)
  |-- /api/search       -> semantic search by ingredients (vector KNN)
  |-- /api/index        -> index recipes into Redis
  |-- /api/scan         -> fridge photo scan + RAG recipe suggestions
  |-- /q/openapi        -> OpenAPI 3.1 spec
  |-- /q/swagger-ui     -> interactive API docs (dev only)
  |-- /q/health         -> health checks
  |-- /q/metrics        -> Prometheus metrics
  |
  +-- RecipeService -> RecipeRepository -> Firestore
  +-- SearchService -> RedisVectorRepository -> Memorystore Redis
  +-- EmbeddingService -> Vertex AI text-embedding-004
  +-- ScanService      -> ImageCacheService (L1) -> GeminiService -> SemanticCacheService (L2) -> SearchService
  +-- GeminiService    -> Vertex AI Gemini 2.5 Flash (vision + text)
  +-- ImageCacheService    -> ImageCacheRepository -> Memorystore Redis (img:{model}:{sha256})
  +-- SemanticCacheService -> RedisCacheRepository -> Memorystore Redis (idx:cache)
  |
  v
GCP Services
  +-- Firestore (recipe storage, 2000 recipes)
  +-- Memorystore Redis 7.2 (HNSW vector index, KNN search)
  +-- Vertex AI (Gemini 2.5 Flash vision+text, text-embedding-004 768d)

Monitoring
  +-- Node Exporter (port 9100) -> system metrics (CPU, memory, disk, network)
  +-- Prometheus (port 9090)    -> scrapes /q/metrics + Node Exporter
  +-- Grafana (port 3000)       -> 20-panel dashboard (RED + USE + semantic cache + image cache)
```

Every response includes an `X-Request-Id` header for log correlation.
Error responses return structured JSON (`message`, `requestId`, `timestamp`), never stack traces.

## Quick Start (local)

```bash
docker-compose up --build
```

Backend: http://localhost:8080/api/status
Frontend: http://localhost:3000

## Seed Recipes

Load 2000 recipes into Firestore. Requires `google-cloud-firestore` Python package.

```bash
pip install google-cloud-firestore
```

Against local emulator (while `quarkus dev` is running):

```bash
FIRESTORE_EMULATOR_HOST=localhost:8081 python scripts/seed_firestore.py
```

Against real GCP Firestore:

```bash
gcloud auth application-default login
python scripts/seed_firestore.py
```

The script uses deterministic document IDs. Running it twice overwrites instead of duplicating.

## Deploy to GCP

```bash
cd terraform && terraform apply          # provision infrastructure
cd .. && ./scripts/update-vault.sh       # update ansible vault with new IPs
cd ansible && ansible-playbook playbook.yml  # deploy containers
```

Tear down after each session:

```bash
cd terraform && terraform destroy
```

## API

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/status | Health check |
| GET | /api/recipes | List recipes (query: limit, offset) |
| GET | /api/recipes/{id} | Get recipe by ID |
| POST | /api/recipes | Create a recipe |
| POST | /api/scan | Upload fridge photo, get recipe suggestions (multipart image) |
| GET | /api/search | Semantic recipe search (query: ingredients, limit) |
| POST | /api/index | Index all recipes into Redis (batch embed + HSET), clears cache |
| GET | /api/cache/stats | Cache statistics for both layers (semantic L2 and image L1) |
| DELETE | /api/cache | Clear both semantic and image caches |
| POST | /api/cache/init | Initialize both cache indexes |
| GET | /q/openapi | OpenAPI 3.1 specification |
| GET | /q/swagger-ui | Interactive API docs (dev profile only) |
| GET | /q/health | Liveness and readiness checks |
| GET | /q/metrics | Prometheus metrics |

## Load Testing

Four k6 scenarios live under `load-tests/`, designed to run from inside
the production VM (localhost is whitelisted by the gateway rate limit).
Results stream into Prometheus via remote write and surface on the
Grafana dashboard during each run.

- Scenario 1: image cache L1 hit rate (100 scans of the same image)
- Scenario 2: search throughput (50 requests, 25 ingredient lists)
- Scenario 3: recipes baseline (500 paginated Firestore reads)
- Scenario 4: scan stress (10 concurrent VUs, 10 different images)

See `load-tests/README.md` for the exact run procedure and
`docs/load-test-results.md` for measured numbers and observations.

## Frontend

Single page app built with React and plain CSS (no component library). Warm Kitchen color theme.

Features:
- Drag and drop or camera photo upload
- Ingredient detection display as tags
- Recipe suggestion cards with ingredient match highlighting
- Cache hit/miss badge on each recipe card
- Scan duration display (demonstrates cache speed difference)
- Error handling with dismissable messages

Local development with Vite proxy (no CORS needed):

```bash
cd frontend && pnpm install && pnpm dev
```

57 tests with Vitest and React Testing Library:

```bash
cd frontend && pnpm test
```

## Project Structure

```
backend/       Kotlin + Quarkus REST API
frontend/      React + Vite (photo upload, recipe display, cache badges)
terraform/     GCP infrastructure (VPC, VM, Redis, Firestore)
ansible/       Deployment automation (Docker, app containers)
monitoring/    Prometheus config, Grafana dashboards and provisioning
scripts/       Helper scripts (image push, vault update, recipe filter)
docs/adr/      Architecture Decision Records
```
