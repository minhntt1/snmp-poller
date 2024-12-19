package com.home.spring_cpe_stats.traffic_manager.application.in;

import com.home.spring_cpe_stats.traffic_manager.domain.entity.TrafficInfo;

import java.util.List;

public interface TrafficManagerUseCase {
    void saveTrafficRecord(List<TrafficInfo> trafficInfoList);
}
