package com.home.network.statistic.poller.igate.gw240.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusWifiStationRepo extends JpaRepository<StatusWifiStationEntity, Long> {
}
