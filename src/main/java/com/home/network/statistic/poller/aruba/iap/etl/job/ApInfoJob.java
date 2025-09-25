package com.home.network.statistic.poller.aruba.iap.etl.job;

import com.home.network.statistic.poller.aruba.iap.etl.service.BaseService;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class ApInfoJob implements Job {
    @Autowired(required = false)
    @Qualifier("apInfoService")
    private BaseService baseService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // job execution context not used yet
        baseService.start();
    }
}
