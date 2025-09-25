package com.home.network.statistic.common.model;


import java.util.Optional;
import java.util.Properties;

public record ListSqlQuery(Properties properties) {
    public String getQueryValue(String id) {
        String val = properties.getProperty(id);
        if (Optional.ofNullable(val).isEmpty()) {
            throw new IllegalArgumentException("No value found for id " + id);
        }
        return val;
    }
}