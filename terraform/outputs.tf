output "vm_ip" {
  value = module.vm.external_ip
}

output "network_name" {
  value = module.networking.network_name
}

output "subnet_name" {
  value = module.networking.subnet_name
}
