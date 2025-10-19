package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class ClientWlanConnectEvent {
    private final Long deviceMac;
    private final String deviceName;
    private final Integer deviceIp;
    private final Long deviceWlanMac;
    private final String dateConnect;
    private final Integer timeSecondConnect;

    public ClientWlanConnectEvent(ArubaAiClientInfoEntity a) {
        this.deviceMac = a.getDeviceMac();
        this.deviceName = Optional.ofNullable(a.getDeviceName()).orElse("");
        this.deviceIp = a.getDeviceIp();
        this.deviceWlanMac = Optional.ofNullable(a.getDeviceWlanMac()).orElse(Long.MIN_VALUE);
        this.dateConnect = a.obtainConnectDate();
        this.timeSecondConnect = a.obtainConnectTime();
    }

    public static String obtainSqlQuery(List<ClientWlanConnectEvent> list) {
        var query = new StringBuilder();

        for (var val : list) {
            if (val == null) continue;

            if (query.isEmpty()) query.append(val.obtainFirstSqlQuery());
            else query.append(val.obtainSqlQuery());
        }

        return query.toString();
    }

    public static List<Object[]> obtainMappedRow(List<ClientWlanConnectEvent> list) {
        return list.stream().map(ClientWlanConnectEvent::obtainMappedRow).toList();
    }

    public Object[] obtainMappedRow() {
        return new Object[] {deviceMac, deviceName, deviceIp, deviceWlanMac, dateConnect, timeSecondConnect};
    }

    public String obtainFirstSqlQuery() {
        return """
                select '%d' as `device_mac`, '%s' as `device_name`, '%d' as `device_ip`, '%d' as `device_wlan_mac`, '%s' as `date_connect`, '%d' as `time_connect`
                """.formatted(this.deviceMac, this.deviceName, this.deviceIp, this.deviceWlanMac, this.dateConnect, this.timeSecondConnect);
    }

    public String obtainSqlQuery() {
        return """
                union all
                select '%d', '%s', '%d', '%d', '%s', '%d'
                """.formatted(this.deviceMac, this.deviceName, this.deviceIp, this.deviceWlanMac, this.dateConnect, this.timeSecondConnect);
    }
}
