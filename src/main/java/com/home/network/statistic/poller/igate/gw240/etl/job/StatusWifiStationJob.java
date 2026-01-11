package com.home.network.statistic.poller.igate.gw240.etl.job;

import com.home.network.statistic.poller.igate.gw240.etl.service.StatusWifiStationService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class StatusWifiStationJob implements Job {
    @Autowired(required = false)
    private StatusWifiStationService statusWifiStationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        statusWifiStationService.start(context);
    }
}
