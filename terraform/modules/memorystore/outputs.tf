output "host" {
  value       = google_redis_instance.main.host
  description = "Internal IP of the Redis instance"
}

output "port" {
  value       = google_redis_instance.main.port
  description = "Redis port"
}
