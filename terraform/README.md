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
- Enables required APIs (Compute, Firestore, Redis, Vertex AI)
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
    Memorystore Redis 7.2 (1GB, basic tier, private access)
  Firestore (native mode)
```

Docker images are hosted on GHCR (GitHub Container Registry), not on GCP.
See docs/adr/004-artifact-registry.md for why.

## Modules

- **networking**: VPC, subnet, firewall rules (SSH on 22 from configurable CIDR, HTTP on 80, Grafana on 3000). Backend on 8080 and Prometheus on 9090 stay internal to the Docker network.
- **vm**: GCE instance with static external IP, SSH access via public key
- **memorystore**: Redis 7.2 instance connected to the VPC, used for semantic cache and vector search
- **firestore**: Document database for recipe storage

## Usage

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# edit terraform.tfvars: set your SSH public key
terraform init
terraform plan
terraform apply
```

## Outputs

After apply, Terraform prints:
- `vm_ip`: SSH and HTTP access to the VM
- `redis_host` + `redis_port`: connection details for the app (internal, not public)
- `network_name`: VPC name

## Destroy

After each dev session to save credits:

```bash
terraform destroy
```

Memorystore costs ~$0.05/hour. Do not leave it running overnight.

## Multi-Region

Change `region` and `zone` in terraform.tfvars to deploy in a different region.
All module resources reference these variables. Nothing is hardcoded to a specific region.
