package com.home.spring_cpe_stats.traffic_manager.adapter.config;

import com.home.spring_cpe_stats.traffic_manager.adapter.exception.ScheduledTaskExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {
    @Bean
    ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.setErrorHandler(new ScheduledTaskExceptionHandler());
        return taskScheduler;
    }
}
