resource "google_redis_instance" "main" {
  name           = "restekoch-redis"
  project        = var.project_id
  region         = var.region
  tier           = "BASIC"
  memory_size_gb = 1
  redis_version  = "REDIS_7_2"

  authorized_network = var.network_id

  redis_configs = {
    maxmemory-policy = "allkeys-lru"
  }
}
