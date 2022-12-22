terraform {
  required_providers {
    sbercloud = {
      source  = "sbercloud.terraform.com/local/sbercloud"
      version = "1.10.0"
    }
  }
}

provider "sbercloud" {
  region     = var.region
  auth_url   = var.auth_url
  access_key = var.access_key
  secret_key = var.secret_key
}

data "sbercloud_enterprise_project" "enterprise_project" {
  name = var.enterprise_project_name
}

resource "sbercloud_vpc" "vpc" {
  name                  = var.vpc_name
  cidr                  = var.vpc_cidr
  enterprise_project_id = data.sbercloud_enterprise_project.enterprise_project.id

  tags = {
    created_by = "terraform"
  }
}

resource "sbercloud_vpc_subnet" "subnet" {
  name = var.subnet_name
  cidr = var.subnet_cidr
  gateway_ip = var.subnet_gateway_ip
  vpc_id     = sbercloud_vpc.vpc.id

  primary_dns   = var.subnet_primary_dns
  secondary_dns = var.subnet_secondary_dns

  dhcp_enable = true

  tags = {
    created_by = "terraform"
  }
}