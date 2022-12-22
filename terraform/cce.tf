# CLOUD CONTAINER ENGINE КЛАСТЕР

data "sbercloud_availability_zones" "cce_masters_az" {}
data "sbercloud_availability_zones" "cce_node_az" {}


resource "sbercloud_cce_cluster" "cce" {
  name                  = var.cce_name
  enterprise_project_id = data.sbercloud_enterprise_project.enterprise_project.id
  cluster_version = "v1.21"
  cluster_type = "VirtualMachine"
  flavor_id = "cce.s1.small"
  vpc_id    = sbercloud_vpc.vpc.id
  subnet_id = sbercloud_vpc_subnet.subnet.id
  container_network_type = "vpc-router"
  authentication_mode    = "rbac"

  masters {
    availability_zone = data.sbercloud_availability_zones.cce_masters_az.names[0]
  }

  tags = {
    created_by = "terraform"
  }
}

resource "sbercloud_cce_node_pool" "cce_node_pool" {
  cluster_id = sbercloud_cce_cluster.cce.id
  name       = var.cce_node_pool_name
  os = "Ubuntu 18.04"
  flavor_id         = "s6.large.2"
  availability_zone = data.sbercloud_availability_zones.cce_node_az.names[0]
  password = var.cce_node_pool_password
  scall_enable             = true
  min_node_count           = 1
  initial_node_count       = 5
  max_node_count           = 10
  scale_down_cooldown_time = 100
  priority                 = 1
  type = "vm"

  root_volume {
    size       = 50
    volumetype = "SAS"
  }

  data_volumes {
    size       = 100
    volumetype = "SAS"
  }

  tags = {
    created_by = "terraform"
  }
}

resource "sbercloud_cce_node" "cce_monitoring_node" {
  cluster_id = sbercloud_cce_cluster.cce.id
  name       = var.cce_monitoring_node_name
  flavor_id         = "s7n.xlarge.4"
  availability_zone = data.sbercloud_availability_zones.cce_node_az.names[0]
  password = var.cce_node_pool_password
  os       = "Ubuntu 18.04"

  root_volume {
    size       = 50
    volumetype = "SAS"
  }

  data_volumes {
    size       = 100
    volumetype = "SSD"
  }

  tags = {
    created_by = "terraform"
    purpose    = "monitoring"
  }
}


resource "sbercloud_vpc_eip" "cce_nat_eip" {
  publicip {
    type = "5_bgp"
  }

  bandwidth {
    name = var.cce_nat_eip_bandwith_name
    size = 8
    share_type = "PER"
    charge_mode = "traffic"
  }

  tags = {
    created_by = "terraform"
  }
}

resource "sbercloud_nat_gateway" "cce_nat_gw" {
  name        = var.cce_nat_gw_name
  description = "NAT for CCE worker nodes and nginx balancer nodes"
  spec = "1"
  vpc_id = sbercloud_vpc.vpc.id
  subnet_id = sbercloud_vpc_subnet.subnet.id
}

resource "sbercloud_nat_snat_rule" "cce_nat_snat" {
  nat_gateway_id = sbercloud_nat_gateway.cce_nat_gw.id
  subnet_id = sbercloud_vpc_subnet.subnet.id
  floating_ip_id = sbercloud_vpc_eip.cce_nat_eip.id
}


data "sbercloud_cce_addon_template" "autoscaler_template" {
  cluster_id = sbercloud_cce_cluster.cce.id
  name = "autoscaler"
  version = "1.21.4"
}

resource "sbercloud_cce_addon" "autoscaler" {
  cluster_id = sbercloud_cce_cluster.cce.id
  template_name = "autoscaler"
  version = "1.21.4"

  values {
    basic = jsondecode(data.sbercloud_cce_addon_template.autoscaler_template.spec).basic
    custom = merge(
      jsondecode(data.sbercloud_cce_addon_template.autoscaler_template.spec).parameters.custom,
      {
        cluster_id = sbercloud_cce_cluster.cce.id
        tenant_id = var.tenant
      }
    )
  }
}

resource "sbercloud_cce_addon" "nginx_ingress" {
  cluster_id    = sbercloud_cce_cluster.cce.id
  template_name = "nginx-ingress"
  version = "2.1.0"
}




output "cce_nat_eip" {
  description = "EIP, from which CCE nodes access internet"
  value       = sbercloud_vpc_eip.cce_nat_eip.address
}

resource "local_file" "kubeconfig" {
  content  = sbercloud_cce_cluster.cce.kube_config_raw
  filename = var.kube_config_path
}
