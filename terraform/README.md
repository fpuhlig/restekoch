# Terraform Infrastructure

Provisions the GCP infrastructure for Restekoch.

## Prerequisites

Some resources must exist before Terraform can run. They are not managed
by Terraform because Terraform needs them to store its own state.

Run `bootstrap.sh` once (from Cloud Shell or locally with gcloud):

```bash
chmod +x bootstrap.sh
./bootstrap.sh
```

This creates:
- The GCP project
- Links the billing account
- Enables required APIs (Compute, Firestore, Redis, Vertex AI, Artifact Registry)
- Creates the GCS bucket for Terraform remote state

Why not in Terraform? Chicken-and-egg problem. Terraform stores its state in the
GCS bucket. It can not create the bucket it needs to store the state for creating
that bucket.

## Architecture

```
GCP Project: restekoch
Region: europe-west1 (configurable via variables)

VPC (restekoch-vpc)
  Subnet (10.0.0.0/24)
    GCE VM (e2-medium, Debian 12, static IP)
      -> Docker containers (backend, frontend, monitoring)
    Memorystore Redis (coming)
  Firestore (coming)
  Artifact Registry (coming)
```

## Modules

- **networking**: VPC, subnet, firewall rules (SSH on 22, HTTP on 80/8080/3000/9090)
- **vm**: GCE instance with static external IP, SSH access via public key

## Usage

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# edit terraform.tfvars: set your SSH public key
terraform init
terraform plan
terraform apply
```

## Destroy

After each dev session to save credits:

```bash
terraform destroy
```

## Multi-Region

Change `region` and `zone` in terraform.tfvars to deploy in a different region.
All module resources reference these variables. Nothing is hardcoded to a specific region.
