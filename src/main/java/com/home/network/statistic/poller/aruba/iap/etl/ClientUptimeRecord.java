package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class ClientUptimeRecord {
    private final Long deviceMac;
    private final String deviceName;
    private Long deviceUptimeSeconds;
    private final Integer deviceIp;

    public ClientUptimeRecord(ArubaAiClientInfoEntity o) {
        this.deviceMac = o.getDeviceMac();
        this.deviceName = o.getDeviceName();
        this.deviceUptimeSeconds = o.getDeviceUptimeSeconds();
        this.deviceIp = o.getDeviceIp();
    }

    public void updateDeviceUptimeSeconds(ArubaAiClientInfoEntity o) {
        this.deviceUptimeSeconds = o.getDeviceUptimeSeconds();
    }

    public static String obtainSqlQuery(HashMap<ClientUptimeRecord, ClientUptimeRecord> list) {
        var query = new StringBuilder();

        for (var val : list.values()) {
            if (val == null) continue;

            if (query.isEmpty()) query.append(val.obtainFirstSqlQuery());
            else query.append(val.obtainSqlQuery());
        }

        return query.toString();
    }

    public static List<Object[]> obtainMappedRow(HashMap<ClientUptimeRecord, ClientUptimeRecord> list) {
        return list.values().stream().map(ClientUptimeRecord::obtainMappedRow).toList();
    }

    public Object[] obtainMappedRow() {
        return new Object[] {deviceMac, deviceName, deviceUptimeSeconds, deviceIp};
    }

    public String obtainFirstSqlQuery() {
        return """
                select '%d' as `device_mac`, '%s' as `device_name`, '%d' as `device_uptime_seconds`, '%d' as `device_ip`
                """.formatted(this.deviceMac, this.deviceName, this.deviceUptimeSeconds, this.deviceIp);
    }

    public String obtainSqlQuery() {
        return """
                union all
                select '%d', '%s', '%d', '%d'
                """.formatted(this.deviceMac, this.deviceName, this.deviceUptimeSeconds, this.deviceIp);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClientUptimeRecord that)) return false;
        return Objects.equals(deviceMac, that.deviceMac) && Objects.equals(deviceName, that.deviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceMac, deviceName);
    }
}
