output "vm_ip" {
  value       = module.vm.external_ip
  description = "Static external IP of the VM"
}

output "redis_host" {
  value       = module.memorystore.host
  description = "Internal IP of the Redis instance"
}

output "redis_port" {
  value       = module.memorystore.port
  description = "Redis port"
}

output "network_name" {
  value       = module.networking.network_name
  description = "VPC network name"
}
