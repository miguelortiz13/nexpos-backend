variable "subscription_id" {
  description = "Suscripción del proyecto (sub-nexpos). En CI llega por ARM_SUBSCRIPTION_ID y puede quedar en null."
  type        = string
  default     = null
}

variable "location" {
  description = "Región principal."
  type        = string
  default     = "eastus2"
}

variable "owner" {
  description = "Responsable (etiqueta Owner)."
  type        = string
  default     = "miguel.ortiz.e13@gmail.com"
}

variable "api_image" {
  description = "Imagen inicial de la API. Las siguientes las despliega el CI (`az containerapp update`)."
  type        = string
  default     = "ghcr.io/miguelortiz13/nexpos-backend:latest"
}
