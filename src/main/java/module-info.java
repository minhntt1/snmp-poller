module com.home.network.statistic {
    requires com.fasterxml.jackson.annotation;
    requires com.zaxxer.hikari;
    requires io.swagger.v3.oas.annotations;
    requires jakarta.persistence;
    requires java.sql;
    requires static lombok;
    requires org.hibernate.orm.core;
    requires org.quartz;
    requires org.snmp4j;
    requires spring.beans;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.context.support;
    requires spring.core;
    requires spring.data.jpa;
    requires spring.jdbc;
    requires spring.orm;
    requires spring.tx;
    requires spring.web;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires spring.jcl;
    requires micrometer.core;
}