# Auth
variable "auth_url" {
  description = "URL to auth"
  type        = string
}

variable "region" {
  description = "Region where account located"
  type        = string
}

variable "access_key" {
  description = "AK"
  type        = string
}

variable "secret_key" {
  description = "SK"
  type        = string
}

variable "tenant" {
  description = "Project ID. Can be found in IAM > Projects"
  type        = string
}

variable "enterprise_project_name" {
  description = "Name of enterprise project"
  type = string
}




# VPC
variable "vpc_name" {
  description = "Name for VPC"
  type        = string
  default     = "vpc"
}

variable "vpc_cidr" {
  description = "IP's range for subnets in VPC"
  type        = string
}

# Subnet
variable "subnet_name" {
  description = "Name for subnet"
  type        = string
  default     = "subnet"
}

variable "subnet_cidr" {
  description = "IP's range in subnet"
  type        = string
}

variable "subnet_gateway_ip" {
  description = "Gateway (router) adress in subnet"
  type        = string
}

variable "subnet_primary_dns" {
  description = "Primary DNS for subnet"
  type        = string
}

variable "subnet_secondary_dns" {
  description = "Secondary DNS for subnet"
  type        = string
}




# Access
variable "admin_ips" {
  description = "IPs to allow access to infra"
  type        = list(any)
}




# MGMT Virtual Machine
variable "mgmt_vm_name" {
  description = "Name for MGMT VM"
  type        = string
  default     = "mgmt_vm_name"
}

variable "mgmt_vm_eip_bandwith_name" {
  description = "EIP for MGMT VM name"
  type        = string
  default     = "mgmt_vm_eip_bandwith_name"
}

variable "mgmt_vm_keypair_name" {
  description = "Keypair for MGMT VM name"
  type        = string
  default     = "mgmt_vm_keypair"
}

variable "mgmt_vm_keypair_public_key" {
  description = "Public key for MGMT VM"
  type        = string
}

variable "mgmt_vm_secgroup_name" {
  description = "Security Group for MGMT VM name"
  type        = string
  default     = "mgmt_vm_secgroup"
}




# RDS
variable "rds_name" {
  description = "Name for RDS instance"
  type        = string
  default     = "rds_name"
}

variable "rds_secgroup_name" {
  description = "Security Group for RDS name"
  type        = string
  default     = "rds_secgroup"
}

variable "rds_password" {
  description = "Password for RDS instance"
  type        = string
}




# CCE
variable "cce_eip_bandwith_name" {
  description = "EIP for CCE name"
  type        = string
  default     = "cce_eip_bandwith_name"
}

variable "cce_nat_eip_bandwith_name" {
  description = "EIP for CCE NAT name"
  type        = string
  default     = "cce_nat_eip_bandwith_name"
}

variable "cce_name" {
  description = "Name for CCE cluster"
  type        = string
  default     = "cce"
}

variable "cce_node_pool_name" {
  description = "Name for CCE node pool"
  type        = string
  default     = "cce-node-pool"
}

variable "cce_monitoring_node_name" {
  description = "Name for CCE monitoring node"
  type        = string
  default     = "cce-monitoring-node"
}

variable "cce_nat_gw_name" {
  description = "Name for CCE NAT"
  type        = string
  default     = "cce_nat_gw_name"
}

variable "cce_node_pool_password" {
  description = "Password for CCE nodes"
  type        = string
}

variable "kube_config_path" {
  description = "Path for kube config file locally"
  type = string
}




# Nginx Load Balancer

variable "cce_lb_nginx_count" {
  description = "Count of nginx lb vms"
  type        = number
  default     = 2
}

variable "cce_lb_nginx_name" {
  description = "Template name for nginx lb server"
  type        = string
  default     = "cce_lb_nginx_"
}

variable "cce_lb_nginx_keypair_name" {
  description = "Name of keypair for all nginx lb servers"
  type        = string
  default     = "cce_lb_nginx_keypair"
}

variable "cce_lb_nginx_keypair_public_key" {
  description = "Public key for CCE node pool"
  type        = string
}

variable "cce_lb_nginx_secgroup_name" {
  description = "Name of nginx lb servers security group"
  type        = string
  default     = "cce_lb_nginx_secgroup"
}

variable "cce_lb_name" {
  description = "Name of load balancer"
  type        = string
  default     = "cce_lb"
}

variable "cce_lb_listener_name" {
  description = "Name of load balancer listener"
  type        = string
  default     = "cce_lb_listener"
}

variable "cce_lb_pool_name" {
  description = "Name of load balancer pool"
  type        = string
  default     = "cce_lb_pool"
}

variable "cce_lb_ip" {
  description = "Inside IP of load balancer"
  type        = string
}
