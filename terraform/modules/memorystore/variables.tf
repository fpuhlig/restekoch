variable "project_id" {
  type        = string
  description = "GCP project ID"
}

variable "region" {
  type        = string
  description = "GCP region for the Redis instance"
}

variable "network_id" {
  type        = string
  description = "VPC network self-link for private access"
}
