output "network_name" {
  value       = google_compute_network.main.name
  description = "VPC network name"
}

output "network_id" {
  value       = google_compute_network.main.id
  description = "VPC network self-link for service connections"
}

output "subnet_name" {
  value       = google_compute_subnetwork.main.name
  description = "Subnet name"
}
