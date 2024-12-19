package com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model;

import com.home.spring_cpe_stats.traffic_manager.domain.entity.TrafficInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public abstract class CommonSnmpResponse {
    private Long timestamp;

    public abstract TrafficInfo toTrafficInfo();
}
