package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiApInfoEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Indicate a record representing count reboot per week, apmac, apname, apIp
 * The above values will be persisted to DB
 */
@Getter
@Setter
public class ApRebootWeeklyCount {
    private final String week;
    private final Long apMac;
    private final String apName;
    private final Integer apIp;
    private int rebootCnt;

    // prev time always < current time
    public ApRebootWeeklyCount(ArubaAiApInfoEntity entity) {
        this.week = entity.obtainWeekDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        this.apMac = entity.getApMac();
        this.apIp = entity.getApIp();
        this.apName = entity.getApName();
        this.rebootCnt = 0;
    }

    public void adjustRebootCnt(ArubaAiApInfoEntity old, ArubaAiApInfoEntity nEW) {
        this.rebootCnt += old.checkUptimeGreater(nEW);
    }

    public static List<Object[]> obtainMappedRow(Map<ApRebootWeeklyCount, ApRebootWeeklyCount> map) {
        return map.values().stream().map(ApRebootWeeklyCount::obtainMappedRow).toList();
    }

    public Object[] obtainMappedRow() {
        return new Object[] {week, apMac, apName, apIp, rebootCnt};
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ApRebootWeeklyCount that)) return false;
        return Objects.equals(week, that.week) && Objects.equals(apMac, that.apMac) && Objects.equals(apName, that.apName) && Objects.equals(apIp, that.apIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(week, apMac, apName, apIp);
    }
}
