package com.home.network.statistic.common.config.quartz;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.autoconfigure.quartz.QuartzTransactionManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class QuartzDatasourceConfig {

    @Bean("quartzSchedulerDs")
    @QuartzDataSource
    @ConfigurationProperties("spring.datasource.quartz-scheduler")
    HikariDataSource quartzSchedulerDs() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean("quartzSchedulerTx")
    @QuartzTransactionManager
    PlatformTransactionManager quartzSchedulerTx() {
        return new JdbcTransactionManager(quartzSchedulerDs());
    }
}
