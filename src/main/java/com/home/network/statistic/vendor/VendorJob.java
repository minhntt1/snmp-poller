package com.home.network.statistic.vendor;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class VendorJob implements Job {
    @Autowired(required = false)
    private VendorService vendorService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        vendorService.run();
    }
}
