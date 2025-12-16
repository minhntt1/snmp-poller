package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    /**
     * can be 1: connect, or 2: disconnect
     */
    private final Integer connectStatus;

    // add connect boolean to determine if new event is connect or disconnect event
    // connect event -> calculate date, time connect based on entity
    // disconnect event -> get current date time as time for disconnect (does not rely on entity's date time, only entity's device mac, name, ip, wlan mac)
    public ClientWlanConnectEvent(ArubaAiClientInfoEntity a, boolean connect) {
        this.deviceMac = a.getDeviceMac();
        this.deviceName = Optional.ofNullable(a.getDeviceName()).orElse("");
        this.deviceIp = a.getDeviceIp();
        this.deviceWlanMac = Optional.ofNullable(a.getDeviceWlanMac()).orElse(Long.MIN_VALUE);

        if (connect) {
            this.dateConnect = a.obtainConnectDate();
            this.timeSecondConnect = a.obtainConnectTime();
            this.connectStatus = 1;
        } else {
            // apply utc time
            var currentDt = LocalDateTime.now(ZoneOffset.UTC);
            this.dateConnect = currentDt.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            this.timeSecondConnect = currentDt.toLocalTime().toSecondOfDay();
            this.connectStatus = 2;
        }
    }


    public static List<Object[]> obtainMappedRow(List<ClientWlanConnectEvent> list) {
        return list.stream().map(ClientWlanConnectEvent::obtainMappedRow).toList();
    }

    public Object[] obtainMappedRow() {
        return new Object[] {deviceMac, deviceName, deviceIp, deviceWlanMac, dateConnect, timeSecondConnect, connectStatus};
    }
}
