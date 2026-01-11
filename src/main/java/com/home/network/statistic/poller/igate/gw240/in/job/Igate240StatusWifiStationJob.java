package com.home.network.statistic.poller.igate.gw240.in.job;

import com.home.network.statistic.poller.igate.gw240.in.service.IngestionService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class Igate240StatusWifiStationJob implements Job {
    @Autowired(required = false)
    private IngestionService ingestionService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ingestionService.pollStatusWifiStation();
    }
}
