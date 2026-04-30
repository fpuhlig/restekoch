# ADR 004: GHCR as Primary Container Registry

Status: accepted (2025-04-24 update: Artifact Registry Terraform module has since been removed entirely; GHCR is the sole registry. Body below retained as historical record; see terraform/README.md and terraform/modules/ for current state.)

## Context

We need somewhere to store Docker images. The VM pulls them to run the app. The professor should be able to pull them when the repo goes public.

Artifact Registry is provisioned in Terraform as a GCP resource. But using it as the primary registry means the VM needs gcloud installed, a service account with storage permissions, and `gcloud auth configure-docker` on every deploy. That is a lot of machinery for pulling two images.

## Decision

GHCR (GitHub Container Registry) as the primary registry. CI pushes images on every merge to main. The VM pulls from GHCR. No authentication needed because the repo is public.

Artifact Registry stays in Terraform as a provisioned resource. It demonstrates Terraform module skills and GCP service knowledge. It is not actively used for image pulls.

## Why

GHCR is free for public repos. The CI pushes with the built-in GITHUB_TOKEN. No extra secrets, no service account keys, no gcloud on the VM.

The professor can pull the images without a GCP account: `docker pull ghcr.io/fpuhlig/restekoch/backend:latest`. That lowers the bar for them to test the project.

The project already has Firestore and Memorystore as PaaS services. Artifact Registry does not add to the PaaS score. Keeping it in Terraform shows we can provision it, but there is no reason to route all Docker traffic through it.

## Trade-offs

GHCR pulls cross the public internet. Slower than Artifact Registry in the same GCP region. For two small images this does not matter. If image size grew to hundreds of MB, revisit this.
