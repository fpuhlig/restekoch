resource "google_compute_address" "main" {
  name    = "restekoch-vm-ip"
  project = var.project_id
  region  = var.region
}

resource "google_compute_instance" "main" {
  name         = "restekoch-vm"
  project      = var.project_id
  zone         = var.zone
  machine_type = "e2-medium"
  tags         = ["restekoch-vm"]

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-12"
      size  = 20
    }
  }

  network_interface {
    network    = var.network
    subnetwork = var.subnet

    access_config {
      nat_ip = google_compute_address.main.address
    }
  }

  metadata = {
    ssh-keys = "restekoch:${var.ssh_pub_key}"
  }

  service_account {
    scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring.write",
    ]
  }
}
