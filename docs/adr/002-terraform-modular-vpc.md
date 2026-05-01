# ADR 002: Modular Terraform with Custom VPC

## Status

Accepted, then refined (2026-04-24): the originally planned `artifact-registry` module was dropped (see ADR 004). The four production modules are `networking`, `vm`, `memorystore`, `firestore`. The text below is the original decision; current state is summarised under *Current state*.

## Context

The project needs GCP infrastructure: a VM, a Redis instance, a Firestore database. Terraform is required by the professor.

Two questions came up early. First, should we write one big `main.tf` or split into modules? Second, should we use the GCP default network or create our own VPC?

## Decision

Modular Terraform with a custom VPC.

Each concern gets its own module: networking, vm, memorystore, firestore. The root `main.tf` wires them together and passes variables between them. (An `artifact-registry` module was originally listed here as "coming"; it was later dropped, see ADR 004.)

We create our own VPC instead of using the default network that comes with every GCP project.

## Current state

Four modules in `terraform/modules/`: `networking`, `vm`, `memorystore`, `firestore`. Open ports on the VM: 22 (SSH from a configurable CIDR), 80 (Gateway), 3000 (Grafana with password). Backend port 8080 and Prometheus 9090 are not exposed externally; the gateway proxies the application, and Prometheus is only reachable from Grafana inside the Docker network.

## Why

Modules keep things readable. When the professor opens the repo, they see four small focused modules instead of one 200-line file. Each module has its own variables, outputs, and a clear responsibility.

The custom VPC is about security and control. The default network has pre-created firewall rules that allow broad access. Our VPC starts empty. We only open the three ports listed under *Current state*. The professor dedicates an entire lecture to IT security and asks exam questions about VPC configuration.

Using variables for project_id, region, and zone means the entire setup can deploy to a different region by changing two values. That is the multi-region capability the project requires.

## Trade-offs

More files to manage. For a project this size, a single main.tf would also work. But the modular structure shows understanding of Terraform best practices and matches how teams work in production.

The GCS state backend requires a bucket that must exist before Terraform can run. That is a chicken-and-egg problem we solve with a bootstrap script.
