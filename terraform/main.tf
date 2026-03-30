terraform {
  required_version = ">= 1.5"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 7.0"
    }
  }

  backend "gcs" {
    bucket = "restekoch-tf-state"
    prefix = "terraform/state"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

module "networking" {
  source = "./modules/networking"

  project_id = var.project_id
  region     = var.region
}

module "vm" {
  source = "./modules/vm"

  project_id  = var.project_id
  region      = var.region
  zone        = var.zone
  network     = module.networking.network_name
  subnet      = module.networking.subnet_name
  ssh_pub_key = var.ssh_pub_key
}
