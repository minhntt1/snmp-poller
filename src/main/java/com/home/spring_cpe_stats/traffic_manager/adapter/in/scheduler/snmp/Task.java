package com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp;

import com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.CommonSnmpRequest;
import com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.CommonSnmpResponse;
import com.home.spring_cpe_stats.traffic_manager.application.service.TrafficManagerService;
import com.home.spring_cpe_stats.traffic_manager.domain.entity.TrafficInfo;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.Snmp;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class Task {
    private final TrafficManagerService trafficManagerService;
    private final CommonSnmpRequest arubaTrafficRequest;
    private final Snmp snmp;

    public Task(TrafficManagerService trafficManagerService,
                @Qualifier("arubaTrafficRequest")
                CommonSnmpRequest arubaTrafficRequest, Snmp snmp) {
        this.trafficManagerService = trafficManagerService;
        this.arubaTrafficRequest = arubaTrafficRequest;
        this.snmp = snmp;
    }

    public void pollAruba() throws IOException {
        log.info("start pollAruba");
        List<TrafficInfo> responseTrafficInfoList = arubaTrafficRequest.getTrafficResponse(snmp)
                .stream()
                .map(CommonSnmpResponse::toTrafficInfo)
                .toList();
        trafficManagerService.saveTrafficRecord(responseTrafficInfoList);
        log.info("end pollAruba");
    }
}
