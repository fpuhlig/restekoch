# Ansible Deployment

Configures the GCP VM and deploys the Restekoch containers.

## What it does

1. **docker role**: Installs Docker and Docker Compose on Debian 12 following the official Docker docs. Adds the deploy user to the docker group.
2. **app role**: Copies the production docker-compose file, pulls the latest images from GHCR, starts the containers.

## Prerequisites

- Ansible installed locally (`pip install ansible`)
- SSH access to the VM (key configured in Terraform)
- Docker images pushed to GHCR (happens automatically on merge to main)
- VM IP and Redis host from Terraform outputs

## Vault Setup

Secrets are encrypted with Ansible Vault. One-time setup:

```bash
cd ansible

# Create a vault password (pick something strong, store it safely)
echo "your-password" > .vault-password
chmod 600 .vault-password

# Create the vault file with values from Terraform
cat > group_vars/vault.yml << EOF
vault_vm_ip: "$(cd ../terraform && terraform output -raw vm_ip)"
vault_redis_host: "$(cd ../terraform && terraform output -raw redis_host)"
EOF

# Encrypt it
ansible-vault encrypt group_vars/vault.yml --vault-password-file .vault-password
```

The `.vault-password` file is in `.gitignore`. Never commit it.

After `terraform destroy` and `terraform apply`, the IPs change. Update the vault:

```bash
cd restekoch
./scripts/update-vault.sh
```

This reads the new Terraform outputs and re-encrypts the vault file automatically.

To edit manually:

```bash
ansible-vault edit group_vars/vault.yml
```

## Usage

Full workflow after `terraform apply`:

```bash
./scripts/update-vault.sh           # update vault with new IPs
cd ansible && ansible-playbook playbook.yml  # deploy
```

No `-e` flags needed. All variables come from `group_vars/all.yml` (references) and `group_vars/vault.yml` (encrypted values).

## Idempotency

Run the playbook twice. The second run should change nothing. Docker is already installed, images are already pulled, containers are already running.

## Variables

| Variable | Where | Encrypted |
|----------|-------|-----------|
| vault_vm_ip | vault.yml | yes |
| vault_redis_host | vault.yml | yes |
| redis_port | all.yml | no |
| ghcr_owner | all.yml | no |
| backend_image | all.yml | no |
| frontend_image | all.yml | no |
