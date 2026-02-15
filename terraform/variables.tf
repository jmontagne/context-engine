variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "GCP region for Cloud Run and Vertex AI"
  type        = string
  default     = "us-central1"
}
