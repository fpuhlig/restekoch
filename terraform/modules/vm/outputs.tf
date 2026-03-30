output "external_ip" {
  value       = google_compute_address.main.address
  description = "Static external IP of the VM"
}

output "instance_name" {
  value       = google_compute_instance.main.name
  description = "VM instance name"
}
