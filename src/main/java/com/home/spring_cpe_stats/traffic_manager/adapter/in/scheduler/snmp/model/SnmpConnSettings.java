package com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SnmpConnSettings {
    private String target;
    private String community;

    private int version;
    private int targetTimeout;
    private int targetRetries;

}
