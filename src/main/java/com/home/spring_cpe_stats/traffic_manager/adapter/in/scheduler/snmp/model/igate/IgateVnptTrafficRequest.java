package com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.igate;


import com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.CommonSnmpRequest;
import com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.CommonSnmpResponse;
import org.snmp4j.Snmp;

import java.util.List;

public class IgateVnptTrafficRequest extends CommonSnmpRequest {
    @Override
    public List<CommonSnmpResponse> getTrafficResponse(Snmp snmp) {
        return List.of();
    }
}
