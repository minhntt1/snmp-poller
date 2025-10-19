package com.home.network.statistic.poller.aruba.iap.etl;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ClientWlanMetricEvent {
    private final String dateMetric;
    private final Integer timeSecondMetric;
    private final Long deviceMac;
    private final String deviceName;
    private final Integer deviceSnr;

    public ClientWlanMetricEvent(ArubaAiClientInfoEntity o) {
        this.dateMetric = o.obtainPollDate();
        this.timeSecondMetric = o.obtainPollTime();
        this.deviceMac = o.getDeviceMac();
        this.deviceName = o.getDeviceName();
        this.deviceSnr = o.getDeviceSnr();
    }

    public static String obtainSqlQuery(List<ClientWlanMetricEvent> list) {
        var query = new StringBuilder();

        for (var val : list) {
            if (val == null) continue;

            if (query.isEmpty()) query.append(val.obtainFirstSqlQuery());
            else query.append(val.obtainSqlQuery());
        }

        return query.toString();
    }

    public static List<Object[]> obtainMappedRow(List<ClientWlanMetricEvent> list) {
        return list.stream().map(ClientWlanMetricEvent::obtainMappedRow).toList();
    }

    public Object[] obtainMappedRow() {
        return new Object[] {deviceMac, deviceName, deviceSnr, dateMetric, timeSecondMetric};
    }

    public String obtainFirstSqlQuery() {
        return """
                select '%d' as `device_mac`, '%s' as `device_name`, '%d' as `device_snr`, '%s' as `date_metric`, '%d' as `time_metric`
                """.formatted(this.deviceMac, this.deviceName, this.deviceSnr, this.dateMetric, this.timeSecondMetric);
    }

    public String obtainSqlQuery() {
        return """
                union all
                select '%d', '%s', '%d', '%s', '%d'
                """.formatted(this.deviceMac, this.deviceName, this.deviceSnr, this.dateMetric, this.timeSecondMetric);
    }
}
