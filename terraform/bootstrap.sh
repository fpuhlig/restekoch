#!/bin/bash
# One-time setup before Terraform can run.
# These resources can not be managed by Terraform because
# Terraform needs them to exist before it can store its state.
#
# Run this once from Cloud Shell or a local terminal with gcloud configured.

set -euo pipefail

PROJECT_ID="restekoch"
REGION="europe-west1"
BILLING_ACCOUNT="" # fill in your billing account ID

# Create project
gcloud projects create "$PROJECT_ID" --name="Restekoch"

# Link billing
gcloud billing projects link "$PROJECT_ID" --billing-account="$BILLING_ACCOUNT"

# Enable required APIs
gcloud services enable \
  compute.googleapis.com \
  firestore.googleapis.com \
  redis.googleapis.com \
  aiplatform.googleapis.com \
  artifactregistry.googleapis.com \
  --project="$PROJECT_ID"

# Create state bucket (Terraform needs this before it can init)
gcloud storage buckets create "gs://${PROJECT_ID}-tf-state" \
  --location="$REGION" \
  --project="$PROJECT_ID"

echo "Done. Now run: cd terraform && terraform init && terraform plan"
