package com.home.network.statistic.common.config.quartz;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;

import java.util.concurrent.*;

public class AppQuartzThreadPool implements ThreadPool {
    protected final Log logger = LogFactory.getLog(getClass());

    private ThreadPoolExecutor executor;

    @Override
    public boolean runInThread(Runnable runnable) {
        try {
            this.executor.execute(runnable);
            return true;
        } catch (RejectedExecutionException ex) {
            logger.error("Task has been rejected by TaskExecutor", ex);
            return false;
        }
    }

    @Override
    public int blockForAvailableThreads() {
        return executor.getCorePoolSize() - executor.getActiveCount();
    }

    @Override
    public void initialize() throws SchedulerConfigException {
        // total thread pool for all quartz scheduler
        // set this value equal to number of concurrent jobs across all schedulers
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(11);
    }

    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        if (waitForJobsToComplete) {
            // shutdown and await
            executor.close();
        } else {
            executor.shutdownNow();
        }
    }

    @Override
    public int getPoolSize() {
        return executor.getCorePoolSize();
    }

    @Override
    public void setInstanceId(String schedInstId) {

    }

    @Override
    public void setInstanceName(String schedName) {

    }

    public void setThreadCount(int count) {
        // called via reflection because spring set threadCount property for thread pool
    }
}
