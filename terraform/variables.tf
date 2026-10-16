variable "region" {
  description = "Region de AWS donde se despliega la infraestructura."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Nombre base de los recursos."
  type        = string
  default     = "franquicias-api"
}

variable "environment" {
  description = "Entorno logical (dev, prod...)."
  type        = string
  default     = "dev"
}

variable "vpc_cidr" {
  description = "Rango CIDR de la VPC."
  type        = string
  default     = "10.20.0.0/16"
}

variable "http_port" {
  description = "Puerto publico del balanceador."
  type        = number
  default     = 80
}

variable "image_tag" {
  description = "Tag de la imagen publicada en ECR."
  type        = string
  default     = "latest"
}

variable "task_cpu" {
  description = "CPU de la tarea Fargate."
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Memoria de la tarea Fargate (MiB)."
  type        = number
  default     = 1024
}

variable "desired_count" {
  description = "Numero de tareas iniciales."
  type        = number
  default     = 2
}

variable "max_capacity" {
  description = "Maximo de tareas del autoescalado."
  type        = number
  default     = 6
}

variable "db_name" {
  description = "Nombre de la base de datos."
  type        = string
  default     = "franquicias"
}

variable "db_username" {
  description = "Usuario de MySQL."
  type        = string
  default     = "franquicias"
}

variable "db_instance_class" {
  description = "Clase de instancia RDS."
  type        = string
  default     = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "Almacenamiento en GiB."
  type        = number
  default     = 20
}

variable "db_engine_version" {
  description = "Version del motor MySQL."
  type        = string
  default     = "8.4"
}
