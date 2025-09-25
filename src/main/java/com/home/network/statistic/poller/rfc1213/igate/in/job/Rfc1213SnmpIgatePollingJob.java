package com.home.network.statistic.poller.rfc1213.igate.in.job;

import com.home.network.statistic.poller.rfc1213.igate.in.service.Rfc1213SnmpIgatePollingService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class Rfc1213SnmpIgatePollingJob implements Job {
    @Autowired(required = false)
    private Rfc1213SnmpIgatePollingService rfc1213SnmpIgatePollingService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        rfc1213SnmpIgatePollingService.pollIfTraffic();
    }
}
