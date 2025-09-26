package com.home.network.statistic.common.config.quartz;


import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class QuartzSchedulerPollDataConfig {
    @Autowired
    private ApplicationContext applicationContext;

    @Qualifier("quartzSchedulerDs")
    @Autowired
    private DataSource dataSource;

    @Qualifier("quartzSchedulerTx")
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    @Qualifier("quartzJobExecutors")
    private Executor quartzJobExecutors;

    @Value("${config-as-scheduler}")
    private Boolean schedulerMode;

    @Bean
    SchedulerFactoryBean quartzSchedulerPollData() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();

        schedulerFactoryBean.setConfigLocation(new ClassPathResource("quartz/quartz-polldata.properties"));

        // if scheduler mode only, not accepting tasks
        if (schedulerMode) {
            log.info("in scheduler only mode");
            var prop = new Properties();
            prop.put("org.quartz.threadPool.class", NoThreadPoolConfig.class.getName());         // NOTE: to not going to accept task
            schedulerFactoryBean.setQuartzProperties(prop);
            schedulerFactoryBean.setAutoStartup(false); // not start scheduler automatically in scheduler only mode
        } else {
            log.info("not in scheduler only mode");
            schedulerFactoryBean.setTaskExecutor(quartzJobExecutors);  // use Spring task executor, same as org.quartz.threadPool.class
            schedulerFactoryBean.setAutoStartup(true); // enable auto start of scheduler after bean initalization
        }

        schedulerFactoryBean.setDataSource(dataSource);         // use data source participate in transaction
        // schedulerFactoryBean.setNonTransactionalDataSource(); // this one in contrast, one another DS for non transactional operation, for performance gain (if not set, it will same as datasource)
        schedulerFactoryBean.setTransactionManager(transactionManager); // set transaction manager to specify spring conn transaction for tx datasource (used for working with job store)
        schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(true);
        schedulerFactoryBean.setOverwriteExistingJobs(false);    // overwrite persisted job data -> if want to use persisted job data, set to false

        // config app context
        var ctx = new AutowiringSpringBeanJobFactory();
        ctx.setApplicationContext(applicationContext);
        schedulerFactoryBean.setJobFactory(ctx);

        return schedulerFactoryBean;
    }
}
