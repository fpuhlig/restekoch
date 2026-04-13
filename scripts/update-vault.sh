#!/bin/bash
# Update Ansible inventory and vault with current Terraform outputs.
# Run after every terraform apply.

set -euo pipefail

INVENTORY_FILE="ansible/inventory/hosts.yml"
VAULT_FILE="ansible/group_vars/vault.yml"
VAULT_PASS="ansible/.vault-password"

if [ ! -f "$VAULT_PASS" ]; then
  echo "No vault password file found at $VAULT_PASS"
  echo "Create one: echo 'your-password' > $VAULT_PASS && chmod 600 $VAULT_PASS"
  exit 1
fi

VM_IP=$(cd terraform && terraform output -raw vm_ip)
REDIS_HOST=$(cd terraform && terraform output -raw redis_host)

# VM IP goes in inventory (Ansible needs it before loading group_vars)
cat > "$INVENTORY_FILE" << EOF
all:
  hosts:
    restekoch-vm:
      ansible_host: "$VM_IP"
      ansible_user: restekoch
      ansible_ssh_private_key_file: "{{ ssh_private_key | default('~/.ssh/id_rsa') }}"
EOF

# Read existing vault values (if vault exists and is encrypted)
GRAFANA_PASS=""
if [ -f "$VAULT_FILE" ]; then
  GRAFANA_PASS=$(ansible-vault view "$VAULT_FILE" --vault-password-file "$VAULT_PASS" 2>/dev/null | grep vault_grafana_password | cut -d'"' -f2 || echo "")
fi

if [ -z "$GRAFANA_PASS" ]; then
  echo "Error: no vault_grafana_password found in vault."
  echo "Set it first: ansible-vault edit ansible/group_vars/vault.yml"
  exit 1
fi

# Secrets go in vault
cat > "$VAULT_FILE" << EOF
vault_redis_host: "$REDIS_HOST"
vault_grafana_password: "$GRAFANA_PASS"
EOF

ansible-vault encrypt "$VAULT_FILE" --vault-password-file "$VAULT_PASS"

echo "Updated:"
echo "  inventory vm_ip: $VM_IP"
echo "  vault redis_host: $REDIS_HOST"
