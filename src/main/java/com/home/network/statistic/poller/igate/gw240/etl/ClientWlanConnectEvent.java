package com.home.network.statistic.poller.igate.gw240.etl;

import com.home.network.statistic.poller.igate.gw240.out.StatusWifiStationRaw;
import com.home.network.statistic.poller.igate.gw240.out.StatusWifiStationWebDataRaw;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@AllArgsConstructor
public class ClientWlanConnectEvent {
    private final Long deviceMac;
    private final Long deviceWlanMac;
    private final String deviceWlanPhyName;
    private final String deviceWlanName;
    private final String dateConnect;
    private final Integer timeSecondConnect;
    /**
     * can be 1: connect, or 2: disconnect
     */
    private final Integer connectStatus;

    public ClientWlanConnectEvent(StatusWifiStationWebDataRaw webDataRaw) {
        this.deviceMac = webDataRaw.toWClientMacLong();
        this.deviceWlanMac = webDataRaw.getSnmpPhysAddr();
        this.deviceWlanName = webDataRaw.toWWlanESSIDHtmlDecode();
        this.deviceWlanPhyName = webDataRaw.getWPhyName();
        this.dateConnect = webDataRaw.getPollDateTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        this.timeSecondConnect = webDataRaw.getPollDateTime().toLocalTime().toSecondOfDay();
        this.connectStatus = 1;
    }

    public ClientWlanConnectEvent(StatusWifiStationWebDataRaw webDataRaw, LocalDateTime batchTimeNoData) {
        this.deviceMac = webDataRaw.toWClientMacLong();
        this.deviceWlanMac = webDataRaw.getSnmpPhysAddr();
        this.deviceWlanName = webDataRaw.toWWlanESSIDHtmlDecode();
        this.deviceWlanPhyName = webDataRaw.getWPhyName();
        this.dateConnect = batchTimeNoData.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        this.timeSecondConnect = batchTimeNoData.toLocalTime().toSecondOfDay();
        this.connectStatus = 2;
    }

    public Object[] toObjectForInsert() {
        return new Object[] {deviceMac, deviceWlanMac, deviceWlanPhyName, deviceWlanName, dateConnect, timeSecondConnect, connectStatus};
    }

    public static List<Object[]> toObjectsForInsert(List<ClientWlanConnectEvent> events) {
        return events.stream().map(ClientWlanConnectEvent::toObjectForInsert).toList();
    }
}
