package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiWlanTrafficEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
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


    public static List<Object[]> obtainMappedRow(Map<ApTrafficHourlyCount, ApTrafficHourlyCount> map) {
        return map.values().stream().map(ApTrafficHourlyCount::obtainMappedRow).toList();
    }

    public Object[] obtainMappedRow() {
        return new Object[] {apDate, apHour, apWlanMac, apWlanEssid, apWlanRxTotal + apWlanTxTotal};
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
