resource "google_artifact_registry_repository" "main" {
  project       = var.project_id
  location      = var.region
  repository_id = "restekoch"
  format        = "DOCKER"
  description   = "Docker images for the Restekoch app"
}
