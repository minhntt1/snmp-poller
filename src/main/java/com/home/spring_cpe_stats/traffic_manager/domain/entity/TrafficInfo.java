package com.home.spring_cpe_stats.traffic_manager.domain.entity;

import lombok.Builder;

@Builder
public class TrafficInfo {
    private String id;
    private Long timestamp;
    private Long clientMac;
    private String clientName;
    private Integer gatewayIpV4;
    private Integer counterRx;
    private Integer counterTx;
    private VendorVo vendor;
}
