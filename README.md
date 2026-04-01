# Restekoch

Scan your fridge, get recipe ideas.

Upload a photo of your ingredients. The app identifies them and suggests matching recipes using RAG and semantic caching.

## Architecture

```
Client (Browser)
  |
  v
Frontend (React + nginx, port 80)
  |  proxies /api/* to backend
  v
Backend (Kotlin + Quarkus, port 8080)
  |-- /api/status    -> health check
  |-- /api/recipes   -> list, get by ID
  |-- /api/scan      -> (coming) image upload
  |-- /api/search    -> (coming) ingredient search
  |
  +-- RecipeService -> RecipeRepository -> Firestore
  +-- ScanService   -> (coming) Gemini Vision
  +-- SearchService -> (coming) Redis Vector Search
  +-- CacheService  -> (coming) Semantic Cache
  |
  v
GCP Services
  +-- Firestore (recipe storage)
  +-- Memorystore Redis (cache + vector search)
  +-- Vertex AI (Gemini Vision, embeddings)
```

## Quick Start (local)

```bash
docker-compose up --build
```

Backend: http://localhost:8080/api/status
Frontend: http://localhost:3000

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

## Project Structure

```
backend/       Kotlin + Quarkus REST API
frontend/      React + Vite
terraform/     GCP infrastructure (VPC, VM, Redis, Firestore)
ansible/       Deployment automation (Docker, app containers)
monitoring/    Prometheus + Grafana (coming)
scripts/       Helper scripts (image push, vault update, recipe filter)
docs/adr/      Architecture Decision Records
```
