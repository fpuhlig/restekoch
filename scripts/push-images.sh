#!/bin/bash
# Build and push Docker images to Artifact Registry.
#
# Prerequisites:
#   - gcloud CLI authenticated
#   - Artifact Registry exists (via terraform apply)
#   - Docker running locally
#
# Usage:
#   ./scripts/push-images.sh

set -euo pipefail

PROJECT_ID="restekoch"
REGION="europe-west1"
REGISTRY="${REGION}-docker.pkg.dev/${PROJECT_ID}/restekoch"

# Authenticate Docker to Artifact Registry
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet

# Build and push backend
echo "Building backend..."
docker build -t "${REGISTRY}/backend:latest" ./backend
echo "Pushing backend..."
docker push "${REGISTRY}/backend:latest"

# Build and push frontend
echo "Building frontend..."
docker build -t "${REGISTRY}/frontend:latest" ./frontend
echo "Pushing frontend..."
docker push "${REGISTRY}/frontend:latest"

# Build and push gateway
echo "Building gateway..."
docker build -t "${REGISTRY}/gateway:latest" ./gateway
echo "Pushing gateway..."
docker push "${REGISTRY}/gateway:latest"

echo "Done. Images:"
echo "  ${REGISTRY}/backend:latest"
echo "  ${REGISTRY}/frontend:latest"
echo "  ${REGISTRY}/gateway:latest"
