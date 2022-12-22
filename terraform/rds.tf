# RELATION DATABASE SERVICE

resource "sbercloud_networking_secgroup" "rds_secgroup" {
  name        = var.rds_secgroup_name
  description = "Security Group for managing access to RDS"
}

resource "sbercloud_networking_secgroup_rule" "rds_secgroup_rule_allow_postgres" {
  direction         = "ingress"
  ethertype         = "IPv4"
  protocol          = "tcp"
  port_range_min    = 5432
  port_range_max    = 5432
  remote_group_id   = sbercloud_cce_cluster.cce.security_group_id
  security_group_id = sbercloud_networking_secgroup.rds_secgroup.id
}


data "sbercloud_availability_zones" "rds_az" {}

data "sbercloud_rds_flavors" "rds_flavor" {
  db_type       = "PostgreSQL"
  db_version    = "13"
  instance_mode = "single"
  vcpus         = 2
  memory        = 4
}

resource "sbercloud_rds_instance" "rds" {
  name                  = var.rds_name
  enterprise_project_id = data.sbercloud_enterprise_project.enterprise_project.id
  flavor                = data.sbercloud_rds_flavors.rds_flavor.flavors[0].name
  vpc_id                = sbercloud_vpc.vpc.id
  subnet_id             = sbercloud_vpc_subnet.subnet.id
  security_group_id     = sbercloud_networking_secgroup.rds_secgroup.id
  availability_zone     = [data.sbercloud_availability_zones.rds_az.names[0]]

  db {
    type     = "PostgreSQL"
    version  = "13"
    password = var.rds_password
  }

  volume {
    type = "ULTRAHIGH"
    size = 40
  }

  backup_strategy {
    start_time = "08:00-09:00"
    keep_days  = 1
  }

  tags = {
    created_by = "terraform"
  }
}
