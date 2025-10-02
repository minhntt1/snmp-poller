package com.home.network.statistic.poller.aruba.iap.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.home.network.statistic.common.util.JsonUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "aruba_iap_device_info_stg_ingest")
@NoArgsConstructor
@Getter
@Setter // REMEMBER CAREFULLY: IF PERSIST OBJECT TO DB, BUT DOESN’T DECLARE SETTER ON OBJECT, HIBERNATE USES REFLECTION TO CREATE OBJECT -> NULL ATTRIBUTE VALUE
@Builder
@AllArgsConstructor
public class ArubaAiClientInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime pollTime;
    private Long deviceMac;
    private Long deviceWlanMac;
    private Integer deviceIp;
    private Integer deviceApIp;
    private String deviceName;
    private Long deviceRx;
    private Long deviceTx;
    private Integer deviceSnr;
    private Long deviceUptimeSeconds;

    public boolean sameDateHour(ArubaAiClientInfoEntity o) {
        return
            this.pollTime.toLocalDate().equals(o.pollTime.toLocalDate()) &&
            this.pollTime.toLocalTime().truncatedTo(ChronoUnit.HOURS).equals(o.pollTime.toLocalTime().truncatedTo(ChronoUnit.HOURS));
    }

    // o: entity with higher timestamp
    // this: entity with lower timestamp
    public long calcDiffTxOldNew(ArubaAiClientInfoEntity o) {
        return this.checkReconnect(o) || o.deviceTx < this.deviceTx ? o.deviceTx : o.deviceTx - this.deviceTx;
    }

    public long calcDiffRxOldNew(ArubaAiClientInfoEntity o) {
        return this.checkReconnect(o) || o.deviceRx < this.deviceRx ? o.deviceRx : o.deviceRx - this.deviceRx;
    }

    // this: event in the past has lower time
    // o: current event has higher time
    public boolean checkReconnect(ArubaAiClientInfoEntity o) {
        // if same device (same mac and name) but different ap ip, ip or wlan -> consider a reconnect
        if (!Objects.equals(this.deviceApIp, o.deviceApIp) ||
            !Objects.equals(this.deviceIp, o.deviceIp) ||
            !Objects.equals(this.deviceWlanMac, o.deviceWlanMac))
            return true;

        // if all above same, checking uptime threshold, if threshold > 10 min (600 secs) then consider a reconnect event
        return Duration.between(this.obtainUptime(), o.obtainUptime()).toSeconds() > 600;
    }

    public LocalDateTime obtainUptime() {
        return pollTime.minusSeconds(this.deviceUptimeSeconds);
    }

    public String obtainConnectDate() {
        return obtainUptime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public Integer obtainConnectTime() {
        return obtainUptime().toLocalTime().toSecondOfDay();
    }

    public String obtainPollDate() {
        return pollTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public Integer obtainPollTime() {
        return pollTime.toLocalTime().toSecondOfDay();
    }

    public Integer obtainPollTimeHour() {
        return pollTime.toLocalTime().truncatedTo(ChronoUnit.HOURS).toSecondOfDay();
    }

    public String obtainJobStateKey() {
        // device job state key relects coulmns in device_dim table
        return "deviceMac_%d_deviceName_%s_ifaceWifi_%d".formatted(this.deviceMac, Optional.ofNullable(this.deviceName).orElse(""), 1);
    }

    public String toJson() {
        return JsonUtil.toJson(this);
    }

    @SneakyThrows
    public static ArubaAiClientInfoEntity from(String json) {
        if (json == null) return null;
        return JsonUtil.fromJson(json, ArubaAiClientInfoEntity.class);
    }
}
