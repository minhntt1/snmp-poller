package com.home.network.statistic.common.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.util.Properties;

@Configuration
@Profile({"dev-executor","prd-executor"})
@EnableJpaRepositories(
        basePackages = {"com.home.network.statistic.poller.aruba.iap.out",
        "com.home.network.statistic.poller.rfc1213.igate.out",
        "com.home.network.statistic.vendor"},
        entityManagerFactoryRef = "appEm",
        transactionManagerRef = "appJpaTx"
)
public class ExecutorDataSourceConfig {
    @Bean("appDs")
    @ConfigurationProperties(prefix = "spring.datasource.statistic-db")
    HikariDataSource appDs() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean("appJpaProp")
    @ConfigurationProperties(prefix = "spring.jpa.statistic-db")
    Properties properties() {
        return new Properties();
    }

    @Bean("appTx")
    JdbcTransactionManager appTx() {
        return new JdbcTransactionManager(appDs());
    }

    @Bean("appJdbcTemplate")
    JdbcTemplate jdbcTemplate() {
        var template = new JdbcTemplate(appDs());
        template.setFetchSize(10_000);  // send 10000 rows max from mysql to
        return template;
    }

    @Bean("appJpaTx")
    JpaTransactionManager platformTransactionManager(@Qualifier("appEm")EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean("appEm")
    LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean() {
        var conf = new LocalContainerEntityManagerFactoryBean();
        conf.setDataSource(appDs());
        conf.setPersistenceUnitName("appPu");
        conf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        conf.setJpaProperties(properties());
        conf.setPackagesToScan(
                "com.home.network.statistic.poller.aruba.iap.out",
                "com.home.network.statistic.poller.rfc1213.igate.out",
                "com.home.network.statistic.vendor");
        return conf;
    }
}
