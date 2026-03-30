variable "project_id" {
  type        = string
  description = "GCP project ID"
}

variable "region" {
  type        = string
  description = "GCP region for the static IP"
}

variable "zone" {
  type        = string
  description = "GCP zone for the VM instance"
}

variable "network" {
  type        = string
  description = "VPC network name"
}

variable "subnet" {
  type        = string
  description = "Subnet name"
}

variable "ssh_pub_key" {
  type        = string
  description = "SSH public key for VM access"
}
