package com.home.network.statistic.poller.igate.gw240.in.job;

import com.home.network.statistic.poller.igate.gw240.in.service.Igate240StatusWifiStationService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class Igate240StatusWifiStationJob implements Job {
    @Autowired(required = false)
    private Igate240StatusWifiStationService igate240StatusWifiStationService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        igate240StatusWifiStationService.pollStatusWifiStation();
    }
}
