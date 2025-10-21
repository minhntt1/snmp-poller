package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class ClientTrafficHourlyCount {
    private final String date;
    private final Integer timeSecond;
    private final Long deviceMac;
    private final String deviceName;
    private long tx;
    private long rx;

    public ClientTrafficHourlyCount(ArubaAiClientInfoEntity a) {
        this.date = a.obtainPollDate();
        this.timeSecond = a.obtainPollTimeHour();
        this.deviceMac = a.getDeviceMac();
        this.deviceName = a.getDeviceName();
        this.tx = 0;
        this.rx = 0;
    }

    public void adjustTraffic(ArubaAiClientInfoEntity old, ArubaAiClientInfoEntity nEW) {
        // always subtract prev state (because it is accumulated)
        this.tx += old.calcDiffTxOldNew(nEW);
        this.rx += old.calcDiffRxOldNew(nEW);
    }

    public static List<Object[]> obtainMappedRow(HashMap<ClientTrafficHourlyCount, ClientTrafficHourlyCount> list) {
        return list.values().stream().map(ClientTrafficHourlyCount::obtainMappedRow).toList();
    }

    public Object[] obtainMappedRow() {
        return new Object[] {date, timeSecond, deviceMac, deviceName, tx + rx};
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClientTrafficHourlyCount that)) return false;
        return Objects.equals(date, that.date) && Objects.equals(timeSecond, that.timeSecond) && Objects.equals(deviceMac, that.deviceMac) && Objects.equals(deviceName, that.deviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, timeSecond, deviceMac, deviceName);
    }
}
