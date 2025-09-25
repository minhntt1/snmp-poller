package com.home.network.statistic.common.config;

import com.home.network.statistic.common.model.ListSqlQuery;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

@Configuration
public class SqlQueryConfig {
    @SneakyThrows
    @Bean(name = "apInfoQuery")
    ListSqlQuery getListApInfoQuery() {
        Properties props = new Properties();
        props.loadFromXML(new ClassPathResource("etl_queries/ap-info-query.xml").getInputStream());
        return new ListSqlQuery(props);
    }

    @SneakyThrows
    @Bean(name = "apWlanTrafficQuery")
    ListSqlQuery getListApWlanTrafficQuery() {
        Properties properties = new Properties();
        properties.loadFromXML(new ClassPathResource("etl_queries/ap-wlan-traffic-query.xml").getInputStream());
        return new ListSqlQuery(properties);
    }

    @SneakyThrows
    @Bean(name = "clientInfoQuery")
    ListSqlQuery getListClientInfoQuery() {
        Properties properties = new Properties();
        properties.loadFromXML(new ClassPathResource("etl_queries/client-info-query.xml").getInputStream());
        return new ListSqlQuery(properties);
    }

    @SneakyThrows
    @Bean(name = "rfc1213IgateQuery")
    ListSqlQuery getListRfc1213IgateQuery() {
        Properties properties = new Properties();
        properties.loadFromXML(new ClassPathResource("etl_queries/rfc1213-igate-query.xml").getInputStream());
        return new ListSqlQuery(properties);
    }
}
