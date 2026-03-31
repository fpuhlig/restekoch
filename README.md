# Restekoch

Scan your fridge, get recipe ideas.

Upload a photo of your ingredients. The app identifies them and suggests matching recipes using RAG and semantic caching.

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
