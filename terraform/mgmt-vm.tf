# MGMT VIRTUAL MACHINE

resource "sbercloud_compute_keypair" "mgmt_vm_keypair" {
  name       = var.mgmt_vm_keypair_name
  public_key = var.mgmt_vm_keypair_public_key
}

resource "sbercloud_networking_secgroup" "mgmt_vm_secgroup" {
  name        = var.mgmt_vm_secgroup_name
  description = "Security Group for managing access to MGMT VM"
}

resource "sbercloud_networking_secgroup_rule" "mgmt_vm_secgroup_rule_allow_ssh" {
  count = length(var.admin_ips)

  direction         = "ingress"
  ethertype         = "IPv4"
  protocol          = "tcp"
  port_range_min    = 22
  port_range_max    = 22
  remote_ip_prefix  = var.admin_ips[count.index]
  security_group_id = sbercloud_networking_secgroup.mgmt_vm_secgroup.id
}


data "sbercloud_availability_zones" "mgmt_vm_az" {}

data "sbercloud_compute_flavors" "mgmt_vm_flavor" {
  availability_zone = data.sbercloud_availability_zones.mgmt_vm_az.names[0]
  performance_type  = "normal"
  cpu_core_count    = 2
  memory_size       = 4
}

data "sbercloud_images_image" "mgmt_vm_image" {
  name        = "Ubuntu 20.04 server 64bit"
  most_recent = true
}

resource "sbercloud_compute_instance" "mgmt_vm" {
  name                  = var.mgmt_vm_name
  enterprise_project_id = data.sbercloud_enterprise_project.enterprise_project.id
  key_pair              = var.mgmt_vm_keypair_name
  image_id              = data.sbercloud_images_image.mgmt_vm_image.id
  flavor_id             = data.sbercloud_compute_flavors.mgmt_vm_flavor.ids[0]
  security_groups       = [var.mgmt_vm_secgroup_name]
  availability_zone     = data.sbercloud_availability_zones.mgmt_vm_az.names[0]
  system_disk_type      = "SSD"
  system_disk_size      = 20

  network {
    uuid = sbercloud_vpc_subnet.subnet.id
  }

  tags = {
    created_by = "terraform"
  }
}

resource "sbercloud_vpc_eip" "mgmt_vm_eip" {
  publicip {
    type = "5_bgp"
  }

  bandwidth {
    name        = var.mgmt_vm_eip_bandwith_name
    size        = 8
    share_type  = "PER"
    charge_mode = "traffic"
  }

  tags = {
    created_by = "terraform"
  }
}

resource "sbercloud_compute_eip_associate" "mgmt_vm_eip_associated" {
  public_ip   = sbercloud_vpc_eip.mgmt_vm_eip.address
  instance_id = sbercloud_compute_instance.mgmt_vm.id
}




output "mgmt_vm_ip" {
  value = sbercloud_vpc_eip.mgmt_vm_eip.address
}
