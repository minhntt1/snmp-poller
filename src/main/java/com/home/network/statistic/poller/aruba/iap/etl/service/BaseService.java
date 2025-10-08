package com.home.network.statistic.poller.aruba.iap.etl.service;

import org.quartz.JobExecutionContext;

public interface BaseService {
    void start(JobExecutionContext context);
}
