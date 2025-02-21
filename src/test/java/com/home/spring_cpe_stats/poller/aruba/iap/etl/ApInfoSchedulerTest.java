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
        jdbcTemplate.execute("insert into ap_dim(ap_mac,ap_name) values(1234,'abcd')");
    }
}
