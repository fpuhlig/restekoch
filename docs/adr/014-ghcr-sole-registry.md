# ADR 014: GHCR as Sole Container Registry, Artifact Registry Module Removed

## Status

Accepted (2026-04-24). Supersedes [ADR 004](004-artifact-registry.md) in part.

## Context

ADR 004 chose GHCR as the primary container registry but kept Google Artifact Registry provisioned in Terraform "for demonstration of Terraform module skills". After deploying the project end-to-end and writing up the project report, that compromise no longer made sense:

1. The Artifact Registry resource cost money (storage) without being used. The student credit budget is 50 USD and every dormant resource eats into it.
2. The four Terraform modules `networking`, `vm`, `memorystore`, `firestore` already demonstrate modular structure, custom VPC, and PaaS provisioning. Adding a fifth unused module did not increase the educational value.
3. Outside reviewers (audit pass on 2026-04-30) flagged the discrepancy between "GHCR is the sole registry" in the body of ADR 004 and "Artifact Registry stays in Terraform" in the same document.

## Decision

Remove the `artifact-registry` Terraform module entirely. The four production modules in `terraform/modules/` are `networking`, `vm`, `memorystore`, `firestore`.

GHCR is the sole container registry. CI pushes three images on every merge to `main`:

- `ghcr.io/<owner>/restekoch-backend:<tag>`
- `ghcr.io/<owner>/restekoch-frontend:<tag>`
- `ghcr.io/<owner>/restekoch-gateway:<tag>`

`<owner>` resolves to the GitHub repository owner via the built-in `${{ github.repository_owner }}` variable. The repository is public; `docker pull` works without GCP credentials.

## Consequences

Positive:

- One less unused GCP resource. Cleaner cost picture.
- ADR 004 and the actual repository state align.
- The `terraform/` directory now contains exactly the four modules referenced throughout the project report.

Negative:

- ADR 004 reads as partially obsolete on first inspection. Mitigated by the explicit "superseded in part by ADR 014" status note.
- Anyone cloning the repo at the ADR-004 commit would still see the full `artifact-registry` module. No regression risk because the removal happened before the assignment was submitted.

## References

- [ADR 004](004-artifact-registry.md): original GHCR-as-primary decision
- `terraform/modules/`: current four modules
- `.github/workflows/ci.yml`: GHCR push job
- `ansible/group_vars/all.yml`: GHCR pull configuration
