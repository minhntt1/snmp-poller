package com.home.spring_cpe_stats.traffic_manager.adapter.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ErrorHandler;

@Slf4j
public class ScheduledTaskExceptionHandler implements ErrorHandler {
    @Override
    public void handleError(Throwable t) {
        log.error("Exception occurred while executing task: {}", t.getMessage(), t);
    }
}
