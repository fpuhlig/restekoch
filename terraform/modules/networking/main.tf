resource "google_compute_network" "main" {
  name                    = "restekoch-vpc"
  project                 = var.project_id
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "main" {
  name          = "restekoch-subnet"
  project       = var.project_id
  region        = var.region
  network       = google_compute_network.main.id
  ip_cidr_range = "10.0.0.0/24"
}

resource "google_compute_firewall" "allow_ssh" {
  name    = "restekoch-allow-ssh"
  project = var.project_id
  network = google_compute_network.main.id

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["restekoch-vm"]
}

resource "google_compute_firewall" "allow_http" {
  name    = "restekoch-allow-http"
  project = var.project_id
  network = google_compute_network.main.id

  allow {
    protocol = "tcp"
    ports    = ["80", "8080", "3000", "9090"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["restekoch-vm"]
}
