package com.home.network.statistic.common.config.quartz;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {
    @Bean
    @Profile({"dev-executor","prd-executor"})
    Executor quartzJobExecutors() {
        return Executors.newFixedThreadPool(10); // 10 threads for 8 jobs, defined in job scheduler file.
    }
}
