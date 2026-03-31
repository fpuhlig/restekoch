# Ansible Deployment

Configures the GCP VM and deploys the Restekoch containers.

## What it does

1. **docker role**: Installs Docker and Docker Compose on Debian 12 following the official Docker docs. Adds the deploy user to the docker group.
2. **app role**: Authenticates to Artifact Registry, copies the production docker-compose file, pulls the latest images, starts the containers.

## Prerequisites

- Ansible installed locally (`pip install ansible`)
- SSH access to the VM (key configured in Terraform)
- Docker images pushed to Artifact Registry
- VM IP from `terraform output vm_ip`
- Redis host from `terraform output redis_host`

## Usage

```bash
cd ansible
ansible-playbook playbook.yml \
  -e "vm_ip=$(cd ../terraform && terraform output -raw vm_ip)" \
  -e "REDIS_HOST=$(cd ../terraform && terraform output -raw redis_host)"
```

## Idempotency

Run the playbook twice. The second run should change nothing. Docker is already installed, images are already pulled, containers are already running.

## Variables

| Variable | Source | Default |
|----------|--------|---------|
| vm_ip | terraform output | required |
| ssh_private_key | your SSH key path | ~/.ssh/id_rsa |
| redis_host | terraform output / env | 10.0.0.1 |
| redis_port | - | 6379 |
| registry_url | built from project_id + region | europe-west1-docker.pkg.dev/restekoch/restekoch |
