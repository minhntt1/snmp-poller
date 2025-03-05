package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientInfoSchedulerJob implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    @Scheduled(fixedRate = 60_000) // 1 min
    public void start() {
         // select from client info stg
        // where mark = 0
        // need to remember old state?
    }
}
