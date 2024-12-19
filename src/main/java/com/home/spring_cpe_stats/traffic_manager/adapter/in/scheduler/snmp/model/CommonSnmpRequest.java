package com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model;

import lombok.*;
import org.snmp4j.Snmp;

import java.io.IOException;
import java.util.List;


@Getter
@Setter
public abstract class CommonSnmpRequest {
    public abstract List<CommonSnmpResponse> getTrafficResponse(Snmp snmp) throws IOException;
}
