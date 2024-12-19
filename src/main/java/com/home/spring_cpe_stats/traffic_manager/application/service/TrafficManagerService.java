package com.home.spring_cpe_stats.traffic_manager.application.service;

import com.home.spring_cpe_stats.traffic_manager.application.in.TrafficManagerUseCase;
import com.home.spring_cpe_stats.traffic_manager.domain.entity.TrafficInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrafficManagerService implements TrafficManagerUseCase {
    @Override
    public void saveTrafficRecord(List<TrafficInfo> trafficInfoList) {

    }
}
