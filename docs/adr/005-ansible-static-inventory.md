# ADR 005: Ansible with Static Inventory

## Status

Accepted

## Context

Ansible needs to know which machines to configure. Two options: a static inventory file where we list the VM IP manually, or a dynamic inventory plugin that queries GCP for running instances.

## Decision

Static inventory. The VM IP is passed as a variable when running the playbook.

## Why

We have one VM. A dynamic inventory plugin that queries the GCP API, parses instance metadata, and filters by labels adds complexity for no benefit. It would also require GCP credentials on the machine running Ansible, and adds a dependency on the `google.cloud` Ansible collection.

The VM IP comes from `terraform output vm_ip`. One command, one variable, done.

If the project grew to multiple VMs (e.g. for load balancing), dynamic inventory would make sense. For one VM it is overhead.

## Trade-offs

After every `terraform destroy` and `terraform apply`, the VM IP changes. The user must pass the new IP to Ansible. That is one extra step but keeps the setup simple.
