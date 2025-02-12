package com.home.spring_cpe_stats.poller.rfc1213.igate.in;

import lombok.AllArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@AllArgsConstructor
public class Rfc1213SnmpIgateResponse {
    private Long at;

    public LocalDateTime toUTC7DateTime() {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(this.at),
                ZoneId.of("UTC+07")
        );
    }
}
