output "network_name" {
  value       = google_compute_network.main.name
  description = "VPC network name"
}

output "subnet_name" {
  value       = google_compute_subnetwork.main.name
  description = "Subnet name"
}
