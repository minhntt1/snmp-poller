package com.home.network.statistic.common.config.quartz;

import org.quartz.simpl.ZeroSizeThreadPool;

public class NoThreadPoolConfig  extends ZeroSizeThreadPool {
    public void setThreadCount(int count) {

    }
}
