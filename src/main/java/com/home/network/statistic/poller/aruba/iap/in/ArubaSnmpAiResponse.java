package com.home.network.statistic.poller.aruba.iap.in;

import lombok.AllArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@AllArgsConstructor
public abstract class ArubaSnmpAiResponse {
    private Long at;

    public LocalDateTime toCurrentLdt() {
        /*
        * The issue:
        * Server timezone is UTC
        * -> jvm timezone is UTC.
        * Client code is convert to ICT (+7) time
        * -> LocalDateTime is date time in +7 zone, (ex: 14:00).
        * When saving to mysql with hibernate, it maps localdatetime to timestamp, timestamp attaches to machine zone (UTC).
        * Connection time zone is utc -> same with jvm zone, no mapping
        * -> save to db time will be still 14:00 (though it is +7 time) -> wrong
        * or we can use: LocalDateTime.now()
        * */
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.at), ZoneId.systemDefault());
    }
}
