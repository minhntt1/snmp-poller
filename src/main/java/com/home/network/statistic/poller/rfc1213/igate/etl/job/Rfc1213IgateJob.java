package com.home.network.statistic.poller.rfc1213.igate.etl.job;

import com.home.network.statistic.poller.rfc1213.igate.etl.service.BaseService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class Rfc1213IgateJob implements Job {
    @Autowired(required = false)
    @Qualifier("rfc1213IgateService")
    private BaseService baseService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        baseService.start();
    }
}
