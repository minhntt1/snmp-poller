package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiWlanTrafficEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Objects;

@Getter
@Setter
public class ApTrafficHourlyCount {
    private final String apDate;
    private final int apHour;  // hour is second
    private final long apWlanMac;
    private final String apWlanEssid;
    private long apWlanRxTotal;
    private long apWlanTxTotal;

    public ApTrafficHourlyCount(ArubaAiWlanTrafficEntity current) {
        this.apDate = current.obtainPollDate();
        this.apHour = current.obtainPollHour();
        this.apWlanMac = current.getWlanMac();
        this.apWlanEssid = current.getWlanEssid();
        this.apWlanRxTotal = 0;
        this.apWlanTxTotal = 0;
    }

    public static String obtainSqlValues(Map<ApTrafficHourlyCount, ApTrafficHourlyCount> map) {
        var sb = new StringBuilder();

        for (var v : map.values()) {
            if (v == null) continue;

            if (sb.isEmpty()) sb.append(v.obtainFirstSqlValues());
            else sb.append(v.obtainSqlValues());
        }

        return sb.toString();
    }

    public String obtainFirstSqlValues() {
        return """
                select
                '%s' as `date`, '%d' as `hour`, '%d' as `wlan_mac`, '%s' as `wlan_essid`, '%d' as `transmission_bytes_val`
                """.formatted(this.apDate, this.apHour, this.apWlanMac, this.apWlanEssid, this.apWlanTxTotal + this.apWlanRxTotal);
    }

    public String obtainSqlValues() {
        return """
                union all
                select '%s', '%d', '%d', '%s', '%d'
                """.formatted(this.apDate, this.apHour, this.apWlanMac, this.apWlanEssid, this.apWlanTxTotal + this.apWlanRxTotal);
    }


    public void updateTraffic(ArubaAiWlanTrafficEntity old, ArubaAiWlanTrafficEntity nEW) {
        // always subtract prev state (because it is accumulated)
        this.apWlanTxTotal += old.calcDiffTxOldNew(nEW);
        this.apWlanRxTotal += old.calcDiffRxOldNew(nEW);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ApTrafficHourlyCount that)) return false;
        return apWlanMac == that.apWlanMac && Objects.equals(apDate, that.apDate) && Objects.equals(apHour, that.apHour) && Objects.equals(apWlanEssid, that.apWlanEssid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apDate, apHour, apWlanMac, apWlanEssid);
    }
}
