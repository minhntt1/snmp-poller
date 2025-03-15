package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class ApInfoSchedulerTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void testExceptionTransaction() {
        jdbcTemplate.execute("insert into ap_dim(ap_mac,ap_name) values(1234,'abcd')");
        throw new RuntimeException();
    }

    @Test
    @Transactional
    void testTransaction() {
        jdbcTemplate.queryForList("SELECT @@transaction_ISOLATION;").forEach(System.out::println);

        jdbcTemplate.execute("insert into aruba_iap_device_info_stg(poll_time,device_mac,device_wlan_mac,device_ip,device_ap_ip,device_name,device_rx,device_tx,device_snr,device_uptime_seconds,\n" +
                "mark) values('2024-01-01',123,123,123,123,'abc',123,13,13,13,0)");

//        jdbcTemplate.execute("select * from ap_dim for update");

        System.out.println();
    }
}
