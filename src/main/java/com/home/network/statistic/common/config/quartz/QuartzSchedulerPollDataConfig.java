package com.home.network.statistic.common.config.quartz;


import lombok.extern.slf4j.Slf4j;
import org.quartz.impl.StdSchedulerFactory;
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

    @Value("${config-as-scheduler}")
    private Boolean schedulerMode;

    @Bean
    SchedulerFactoryBean quartzSchedulerPollData() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        var quartzProp = new Properties();

        schedulerFactoryBean.setConfigLocation(new ClassPathResource("quartz/quartz-polldata.properties"));

        if (schedulerMode) {
            // if scheduler mode only, not accepting tasks
            log.info("in scheduler only mode");

            quartzProp.put(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, NoQuartzThreadPool.class.getName());         // NOTE: to not going to accept task
            schedulerFactoryBean.setAutoStartup(false); // not start scheduler automatically in scheduler only mode
        } else {
            log.info("not in scheduler only mode");

            quartzProp.put(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, AppQuartzThreadPool.class.getName());         // custom thread pool for batch acquisition
            schedulerFactoryBean.setAutoStartup(true); // enable auto start of scheduler after bean initalization
        }

        schedulerFactoryBean.setQuartzProperties(quartzProp);   // set quartz custom properties
        schedulerFactoryBean.setNonTransactionalDataSource(dataSource); // data source with quartz managed's transaction
        schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(true);
        schedulerFactoryBean.setOverwriteExistingJobs(false);    // overwrite persisted job data -> if want to use persisted job data, set to false

        // config app context
        var ctx = new AutowiringSpringBeanJobFactory();
        ctx.setApplicationContext(applicationContext);
        schedulerFactoryBean.setJobFactory(ctx);

        return schedulerFactoryBean;
    }
}
