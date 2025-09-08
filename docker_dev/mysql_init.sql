CREATE USER IF NOT EXISTS 'netstats'@'%' IDENTIFIED BY 'abc123';
GRANT ALL PRIVILEGES ON network_statistics.* TO 'netstats'@'%';

CREATE DATABASE `network_statistics` /*!40100 DEFAULT CHARACTER SET latin1 */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `network_statistics`;

CREATE TABLE `ap_dim` (
  `ap_key` int NOT NULL AUTO_INCREMENT,
  `ap_mac` bigint NOT NULL,
  `ap_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ap_key`),
  KEY `search` (`ap_mac`,`ap_name`,`ap_key` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1
/*!50100 PARTITION BY HASH (`ap_key`)
PARTITIONS 5 */;

CREATE TABLE `ap_reboot_cnt_per_week_fact` (
  `date_key` int NOT NULL,
  `ap_key` int NOT NULL,
  `ip_key` int NOT NULL,
  `count_reboot` int NOT NULL,
  PRIMARY KEY (`date_key`,`ap_key`,`ip_key`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1
/*!50100 PARTITION BY KEY (date_key,ap_key,ip_key)
PARTITIONS 5 */;

CREATE TABLE `aruba_iap_ap_info_stg` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `poll_time` datetime NOT NULL,
  `ap_mac` bigint NOT NULL,
  `ap_name` varchar(255) DEFAULT NULL,
  `ap_ip` int NOT NULL,
  `ap_model` varchar(255) NOT NULL,
  `ap_uptime_seconds` bigint NOT NULL,
  `mark` tinyint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `search_mark` (`mark`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1
/*!50100 PARTITION BY HASH (`id`)
PARTITIONS 5 */;

CREATE TABLE `aruba_iap_device_info_stg` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `poll_time` datetime NOT NULL,
  `device_mac` bigint NOT NULL,
  `device_wlan_mac` bigint DEFAULT NULL,
  `device_ip` int NOT NULL,
  `device_ap_ip` int DEFAULT NULL,
  `device_name` varchar(255) DEFAULT NULL,
  `device_rx` bigint NOT NULL,
  `device_tx` bigint NOT NULL,
  `device_snr` int DEFAULT NULL,
  `device_uptime_seconds` bigint DEFAULT NULL,
  `mark` tinyint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `search_mark` (`mark`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1
/*!50100 PARTITION BY HASH (`id`)
PARTITIONS 5 */;

CREATE TABLE `aruba_iap_wlan_traffic_stg` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `poll_time` datetime NOT NULL,
  `wlan_ap_mac` bigint NOT NULL,
  `wlan_essid` varchar(255) NOT NULL,
  `wlan_mac` bigint NOT NULL,
  `wlan_tx` bigint NOT NULL,
  `wlan_rx` bigint NOT NULL,
  `mark` tinyint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `search_mark` (`mark`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1
/*!50100 PARTITION BY HASH (`id`)
PARTITIONS 5 */;

CREATE TABLE `date_dim` (
  `date_key` int NOT NULL AUTO_INCREMENT,
  `date` date NOT NULL,
  PRIMARY KEY (`date_key`),
  KEY `search` (`date`,`date_key`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1
/*!50100 PARTITION BY KEY (date_key)
PARTITIONS 5 */;

CREATE TABLE `device_dim` (
  `device_key` int NOT NULL AUTO_INCREMENT,
  `device_mac` bigint NOT NULL,
  `device_name` varchar(255) DEFAULT NULL,
  `device_iface_wifi` tinyint NOT NULL,
  PRIMARY KEY (`device_key`),
  KEY `search` (`device_iface_wifi`,`device_mac`,`device_name`,`device_key` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1
/*!50100 PARTITION BY HASH (`device_key`)
PARTITIONS 5 */;

CREATE TABLE `device_traffic_by_hour_fact` (
  `date_key` int NOT NULL,
  `time_key` int NOT NULL,
  `device_key` int NOT NULL,
  `transmission_bytes` bigint NOT NULL,
  PRIMARY KEY (`date_key`,`time_key`,`device_key`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1
/*!50100 PARTITION BY KEY (date_key,time_key,device_key)
PARTITIONS 5 */;

CREATE TABLE `device_wlan_connections_fact` (
  `date_key` int NOT NULL,
  `time_key` int NOT NULL,
  `device_key` int NOT NULL,
  `device_ip_key` int NOT NULL,
  `ap_key` int NOT NULL,
  `iface_key` int NOT NULL,
  `vendor_key` int NOT NULL,
  `ap_vendor_key` int NOT NULL,
  PRIMARY KEY (`date_key`,`time_key`,`device_key`,`device_ip_key`,`ap_key`,`iface_key`,`vendor_key`,`ap_vendor_key`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1
/*!50100 PARTITION BY KEY (date_key,time_key,device_key,device_ip_key,ap_key,iface_key,vendor_key,ap_vendor_key)
PARTITIONS 5 */;

CREATE TABLE `device_wlan_metrics_fact` (
  `date_key` int NOT NULL,
  `time_key` int NOT NULL,
  `device_key` int NOT NULL,
  `snr` int NOT NULL,
  PRIMARY KEY (`date_key`,`time_key`,`device_key`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1
/*!50100 PARTITION BY KEY (date_key,time_key,device_key)
PARTITIONS 5 */;

CREATE TABLE `device_wlan_uptime_fact` (
  `device_key` int NOT NULL,
  `ip_key` int NOT NULL,
  `uptime_seconds` bigint NOT NULL,
  PRIMARY KEY (`device_key`,`ip_key`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1
/*!50100 PARTITION BY KEY (device_key,ip_key)
PARTITIONS 5 */;

CREATE TABLE `gw_iface_dim` (
  `iface_key` int NOT NULL AUTO_INCREMENT,
  `iface_mac` bigint NOT NULL,
  `iface_name` varchar(255) DEFAULT NULL,
  `iface_phy_name` varchar(255) DEFAULT NULL,
  `iface_remark` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`iface_key`),
  KEY `search` (`iface_mac`,`iface_phy_name`,`iface_name`,`iface_key` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1 COMMENT='for iface table, name means wifi essid string (if detected), if no essid detected, use iface_phy_name (eth0,eth1)'
/*!50100 PARTITION BY HASH (`iface_key`)
PARTITIONS 5 */;

CREATE TABLE `iface_traffic_by_hour_fact` (
  `date_key` int NOT NULL,
  `time_key` int NOT NULL,
  `iface_key` int NOT NULL,
  `transmission_bytes` bigint NOT NULL,
  PRIMARY KEY (`date_key`,`time_key`,`iface_key`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1
/*!50100 PARTITION BY KEY (date_key,time_key,iface_key)
PARTITIONS 5 */;

CREATE TABLE `ip_dim` (
  `ip_key` int NOT NULL AUTO_INCREMENT,
  `ipv4` int DEFAULT NULL,
  `ipv6` bigint DEFAULT NULL,
  PRIMARY KEY (`ip_key`),
  KEY `search4` (`ipv4`,`ip_key` DESC),
  KEY `search6` (`ipv6`,`ip_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1
/*!50100 PARTITION BY HASH (`ip_key`)
PARTITIONS 5 */;

CREATE TABLE `rfc1213_iftable_traffic_stg` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `poll_time` datetime NOT NULL,
  `if_index` int NOT NULL,
  `if_descr` varchar(255) NOT NULL,
  `if_phys_address` bigint NOT NULL,
  `if_admin_status` enum('1','2','3') NOT NULL,
  `if_oper_status` enum('1','2','3') NOT NULL,
  `if_in_octets` bigint NOT NULL,
  `if_out_octets` bigint NOT NULL,
  `ip_ad_ent_addr` int DEFAULT NULL,
  `mark` tinyint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `search_mark` (`mark`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1
/*!50100 PARTITION BY HASH (`id`)
PARTITIONS 5 */;

CREATE TABLE `time_dim` (
  `time_key` int NOT NULL AUTO_INCREMENT,
  `time` int NOT NULL,
  PRIMARY KEY (`time_key`),
  KEY `search` (`time`,`time_key`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1
/*!50100 PARTITION BY KEY (time_key)
PARTITIONS 5 */;

CREATE TABLE `vendor_dim` (
  `vendor_key` int NOT NULL AUTO_INCREMENT,
  `vendor_prefix` int NOT NULL,
  `vendor_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`vendor_key`),
  KEY `search` (`vendor_prefix`,`vendor_key` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1
/*!50100 PARTITION BY HASH (`vendor_key`)
PARTITIONS 5 */;

FLUSH PRIVILEGES;

