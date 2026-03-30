variable "project_id" {
  type        = string
  description = "GCP project ID"
  default     = "restekoch"
}

variable "region" {
  type        = string
  description = "GCP region for all resources"
  default     = "europe-west1"
}

variable "zone" {
  type        = string
  description = "GCP zone for the VM"
  default     = "europe-west1-b"
}

variable "ssh_pub_key" {
  type        = string
  description = "SSH public key for VM access"
}
