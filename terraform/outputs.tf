output "alb_dns_name" {
  description = "Nombre publico del balanceador: es la URL base de la API desplegada."
  value       = "http://${aws_lb.app.dns_name}:${var.http_port}"
}

output "api_base_url" {
  description = "URL base para las peticiones a la API."
  value       = "http://${aws_lb.app.dns_name}:${var.http_port}/api"
}

output "health_check_url" {
  description = "Endpoint de salud exposed por actuator."
  value       = "http://${aws_lb.app.dns_name}:${var.http_port}/actuator/health"
}

output "ecs_cluster_name" {
  description = "Cluster ECS donde corre el servicio."
  value       = aws_ecs_cluster.app.name
}

output "ecs_service_name" {
  description = "Servicio ECS del proyecto."
  value       = aws_ecs_service.app.name
}

output "ecr_repository_url" {
  description = "Repositorio ECR donde publicar la imagen."
  value       = aws_ecr_repository.app.repository_url
}

output "ecr_push_command" {
  description = "Comandos para construir y publicar la imagen."
  value = join(" ", [
    "docker build -t ${aws_ecr_repository.app.repository_url}:${var.image_tag} -t franquicias-api .",
    "&& docker push ${aws_ecr_repository.app.repository_url}:${var.image_tag}",
    "&& terraform apply -var 'region=${var.region}' -var 'image_tag=${var.image_tag}'",
  ])
}

output "db_endpoint" {
  description = "Endpoint de MySQL (host:port)."
  value       = "${aws_db_instance.app.address}:${aws_db_instance.app.port}"
}

output "db_name" {
  description = "Nombre de la base de datos."
  value       = aws_db_instance.app.db_name
}

output "db_secret_arn" {
  description = "ARN del secreto con las credenciales de MySQL."
  value       = aws_secretsmanager_secret.db.arn
}

output "log_group_name" {
  description = "Grupo de logs de CloudWatch."
  value       = aws_cloudwatch_log_group.app.name
}
