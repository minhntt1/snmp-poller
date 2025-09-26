package com.home.network.statistic.common.config;

import lombok.extern.slf4j.Slf4j;
import org.snmp4j.Snmp;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Profile({"dev-executor", "prd-executor"})
@Configuration
@Slf4j
public class SnmpConfig {
    @Bean(destroyMethod = "close")
    Snmp snmpUdpListener() throws IOException {
        log.info("Opening snmp listener");
        Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
        snmp.listen();
        return snmp;
    }
}
