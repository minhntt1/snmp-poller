package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiApInfoEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;
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

    public static String obtainSqlValues(Map<ApRebootWeeklyCount, ApRebootWeeklyCount> map) {
        var res = new StringBuilder();

        for (var apRebootValue : map.values()) {
            if (apRebootValue == null) continue;

            if (res.isEmpty()) res.append(apRebootValue.obtainFirstSqlValues());
            else res.append(apRebootValue.obtainSqlValues());
        }

        return res.toString();
    }

    public String obtainFirstSqlValues() {
        return """
                select
                '%s' as `ap_week`, '%d' as `ap_mac`, '%s' as `ap_name`, '%d' as `ap_ip`, '%d' as `reboot_cnt`
                """.formatted(this.week, this.apMac, this.apName, this.apIp, this.rebootCnt);
    }

    public String obtainSqlValues() {
        // sql value ordering:
        // 1st -- ap week
        // 2nd -- ap mac
        // 3rd -- ap name
        // 4th -- ap ip
        // 5th -- reboot cnt
        return """
                union all
                select '%s', '%d', '%s', '%d', '%d'
                """.formatted(this.week, this.apMac, this.apName, this.apIp, this.rebootCnt);
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
