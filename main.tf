terraform {
  backend "local" {}
  required_providers {
    google = {
      source = "hashicorp/google"
    }
  }
}

provider "google" {
  credentials = file("terraform-serv-acct-keys.json")
  project     = var.GCP_PROJECT
  region      = var.GCP_REGION
  zone        = var.GCP_ZONE
}

########################### Create Bucket For Storage ################################
## Create Storage Buckets for Cloud Function Files
resource "google_storage_bucket" "cloud_function_bucket" {
  name          = "${var.GCP_PROJECT}-cloud-function-bucket"
  location      = var.GCP_REGION
  force_destroy = false
  storage_class = "STANDARD"
}

## Upload kafka_producer_zipfile into bucket
resource "google_storage_bucket_object" "kafka_producer_zipfile" {
  name   = "main_requirements.zip"
  bucket = google_storage_bucket.cloud_function_bucket.name
  source = "main_requirements.zip"
}

## Create Storage Buckets for Flink Files
resource "google_storage_bucket" "flink_bucket" {
  name          = "${var.GCP_PROJECT}-flink-bucket"
  location      = var.GCP_REGION
  force_destroy = false
  storage_class = "STANDARD"
}

########################### Create Networking ########################################
## Create VPC Network
resource "google_compute_network" "vpc_network" {
  project                 = var.GCP_PROJECT
  name                    = "test-network"
  auto_create_subnetworks = false
  mtu                     = 1460
}

## Create us-central1 subnet on VPC Network
resource "google_compute_subnetwork" "us_central_subnet" {
  name          = "us-central1-subnet"
  ip_cidr_range = "10.128.0.0/20"
  region        = var.GCP_REGION
  network       = google_compute_network.vpc_network.id
}

## Create Firewall Rule to allow SSH through IAP
resource "google_compute_firewall" "allow_ssh_from_iap" {
  project   = var.GCP_PROJECT
  name      = "allow-ssh-from-iap"
  network   = google_compute_network.vpc_network.id
  direction = "INGRESS"
  priority  = 1000

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["35.235.240.0/20"]
}

## Create Firewall Rule to allow kafka broker
resource "google_compute_firewall" "allow_kafka_port" {
  project   = var.GCP_PROJECT
  name      = "allow-kafka-port"
  network   = google_compute_network.vpc_network.id
  direction = "INGRESS"
  priority  = 1000

  allow {
    protocol = "tcp"
    ports    = ["9092"]
  }

  source_ranges = ["0.0.0.0/0"]
}

## Create Cloud NAT gateway to allow Private VM connection to internet
resource "google_compute_router" "router" {
  name    = "nat-router-us-central1"
  region  = var.GCP_REGION
  network = google_compute_network.vpc_network.id
}

resource "google_compute_router_nat" "nat" {
  name                               = "nat-config"
  router                             = google_compute_router.router.name
  region                             = var.GCP_REGION
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"
}
##################################################################################

# ################ 
# ## Create Kafka VM from Bitnami Image from Marketplace (Cannot be automated on Terraform)
# ################

############################## Create Flink VM ######################################
## Create VM for Flink
resource "google_compute_instance" "flink_vm" {
  name         = "flinkvm"
  machine_type = "n2-standard-4"

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-11"
      size  = 100
    }
  }

  network_interface {
    network    = google_compute_network.vpc_network.id
    subnetwork = google_compute_subnetwork.us_central_subnet.id
  }

}
##################################################################################

################## Serverless Cloud Scheduler Section ############################
## Create Serverless VPC connector to VPC created above for cloud function
resource "google_vpc_access_connector" "serverless_vpc_connector" {
  name          = "serverless-vpc-connector"
  ip_cidr_range = "10.8.0.0/28"
  region        = var.GCP_REGION
  network       = google_compute_network.vpc_network.id
  min_instances = 2
  max_instances = 3
  machine_type  = "f1-micro"
}

## Create Pub/Sub Topic
resource "google_pubsub_topic" "getVehiclePositionTriggerTopic" {
  name = "getVehiclePositionsTrigger"
}


## Create Template Cloud Function with configuration
resource "google_cloudfunctions_function" "function1" {
  name        = "function-1"
  runtime     = "python39"
  available_memory_mb          = 256
  event_trigger{
    event_type = "google.pubsub.topic.publish"
    resource = "projects/${var.GCP_PROJECT}/topics/${google_pubsub_topic.getVehiclePositionTriggerTopic.name}"
  }
  min_instances = 0
  max_instances = 1
  ingress_settings = "ALLOW_INTERNAL_AND_GCLB"
  vpc_connector = "${google_vpc_access_connector.serverless_vpc_connector.name}"
  vpc_connector_egress_settings = "PRIVATE_RANGES_ONLY"
  entry_point = "getVehiclePositions"  
  source_archive_bucket = google_storage_bucket.cloud_function_bucket.name
  source_archive_object = google_storage_bucket_object.kafka_producer_zipfile.name
}

## Create Cloud Scheduler Job
resource "google_cloud_scheduler_job" "job" {
  name        = "getVehiclePositionsSchedule"
  schedule    = "*/2 5-23 * * *"
  time_zone   = "America/Chicago"
  region      = var.GCP_REGION

  pubsub_target {
    topic_name = google_pubsub_topic.getVehiclePositionTriggerTopic.id
    data       = base64encode("hello")
  }
}

################## Artifact Registry Section ############################
resource "google_artifact_registry_repository" "my-repo" {
  location      = "us-central1"
  repository_id = "my-repo"
  description   = "example docker repository"
  format        = "DOCKER"
}

##################### GKE Section ##########################

