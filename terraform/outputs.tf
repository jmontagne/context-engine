output "cloud_run_url" {
  description = "Cloud Run service URL"
  value       = google_cloud_run_v2_service.app.uri
}

output "artifact_registry" {
  description = "Docker registry path"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/context-engine"
}
