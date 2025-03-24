package com.home.spring_cpe_stats.common.config;

import com.home.spring_cpe_stats.common.model.ListSqlQuery;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class SqlQueryConfig {
    @SneakyThrows
    @Bean(name = "apInfoQuery")
    ListSqlQuery getListApInfoQuery() {
        Properties props = new Properties();
        props.loadFromXML(getClass().getClassLoader().getResourceAsStream("etl_queries/ap-info-query.xml"));
        return new ListSqlQuery(props);
    }

    @SneakyThrows
    @Bean(name = "apWlanTrafficQuery")
    ListSqlQuery getListApWlanTrafficQuery() {
        Properties properties = new Properties();
        properties.loadFromXML(getClass().getClassLoader().getResourceAsStream("etl_queries/ap-wlan-traffic-query.xml"));
        return new ListSqlQuery(properties);
    }

    @SneakyThrows
    @Bean(name = "clientInfoQuery")
    ListSqlQuery getListClientInfoQuery() {
        Properties properties = new Properties();
        properties.loadFromXML(getClass().getClassLoader().getResourceAsStream("etl_queries/client-info-query.xml"));
        return new ListSqlQuery(properties);
    }

    @SneakyThrows
    @Bean(name = "rfc1213IgateQuery")
    ListSqlQuery getListRfc1213IgateQuery() {
        Properties properties = new Properties();
        properties.loadFromXML(getClass().getClassLoader().getResourceAsStream("etl_queries/rfc1213-igate-query.xml"));
        return new ListSqlQuery(properties);
    }
}
