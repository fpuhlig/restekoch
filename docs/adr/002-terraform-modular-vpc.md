# ADR 002: Modular Terraform with Custom VPC

Status: accepted

## Context

The project needs GCP infrastructure: a VM, a Redis instance, a Firestore database, and an Artifact Registry. Terraform is required by the professor.

Two questions came up early. First, should we write one big main.tf or split into modules? Second, should we use the GCP default network or create our own VPC?

## Decision

Modular Terraform with a custom VPC.

Each concern gets its own module: networking, vm, memorystore (coming), firestore (coming), artifact-registry (coming). The root main.tf wires them together and passes variables between them.

We create our own VPC instead of using the default network that comes with every GCP project.

## Why

Modules keep things readable. When the professor opens the repo, they see five small focused modules instead of one 200-line file. Each module has its own variables, outputs, and a clear responsibility.

The custom VPC is about security and control. The default network has pre-created firewall rules that allow broad access. Our VPC starts empty. We only open the ports we need (22 for SSH, 80/8080 for the app, 3000/9090 for monitoring). The professor dedicates an entire lecture to IT security and asks exam questions about VPC configuration.

Using variables for project_id, region, and zone means the entire setup can deploy to a different region by changing two values. That is the multi-region capability the project requires.

## Trade-offs

More files to manage. For a project this size, a single main.tf would also work. But the modular structure shows understanding of Terraform best practices and matches how teams work in production.

The GCS state backend requires a bucket that must exist before Terraform can run. That is a chicken-and-egg problem we solve with a bootstrap script.
