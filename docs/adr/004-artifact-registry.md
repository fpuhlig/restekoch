# ADR 004: Artifact Registry for Docker Images

Status: accepted

## Context

We need a place to store Docker images so Ansible can pull them onto the VM. Three options: Docker Hub, GitHub Container Registry (GHCR), or Google Artifact Registry.

## Decision

Artifact Registry in the same GCP project and region as the VM.

## Why

The VM pulls images from the registry. If the registry is in the same GCP region, the pull is fast and free (no egress charges). Docker Hub and GHCR would mean pulling images over the public internet, which is slower and could cost egress fees.

Artifact Registry also integrates with GCP IAM. The VM's service account can authenticate automatically without storing Docker Hub credentials.

Keeping everything in one GCP project makes Terraform simpler. One provider, one project, one set of permissions.

## Trade-offs

Artifact Registry is GCP-specific. If we ever moved away from GCP, the images would need to be pushed elsewhere. For a university project that does not matter.
