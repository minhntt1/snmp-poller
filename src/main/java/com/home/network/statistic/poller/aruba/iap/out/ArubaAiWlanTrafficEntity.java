package com.home.network.statistic.poller.aruba.iap.out;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.home.network.statistic.common.util.JsonUtil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "aruba_iap_wlan_traffic_stg_ingest")
@NoArgsConstructor
@Getter
@Setter // REMEMBER CAREFULLY: IF PERSIST OBJECT TO DB, BUT DOESN’T DECLARE SETTER ON OBJECT, HIBERNATE USES REFLECTION TO CREATE OBJECT -> NULL ATTRIBUTE VALUE
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArubaAiWlanTrafficEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime pollTime;
    private Long wlanApMac;
    private String wlanEssid;
    private Long wlanMac;
    private Long wlanRx;
    private Long wlanTx;

    public boolean sameDateHour(ArubaAiWlanTrafficEntity o) {
        return
            this.pollTime.toLocalDate().equals(o.pollTime.toLocalDate()) &&
            this.pollTime.toLocalTime().truncatedTo(ChronoUnit.HOURS).equals(o.pollTime.toLocalTime().truncatedTo(ChronoUnit.HOURS));
    }

    // o: entity with higher timestamp
    // this: entity with lower timestamp
    public long calcDiffTxOldNew(ArubaAiWlanTrafficEntity o) {
        return o.wlanTx < this.wlanTx ? o.wlanTx : o.wlanTx - this.wlanTx;
    }

    public long calcDiffRxOldNew(ArubaAiWlanTrafficEntity o) {
        return o.wlanRx < this.wlanRx ? o.wlanRx : o.wlanRx - this.wlanRx;
    }

    public String obtainPollDate() {
        return pollTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public int obtainPollHour() {
        // equivalent to sql: time_to_sec(date_format(time(poll_time),'%H:00:00'))
        return pollTime.toLocalTime().truncatedTo(ChronoUnit.HOURS).toSecondOfDay();
    }

    public String obtainJobApStateKey() { // ap wlan state reflects wlan mac, name in gw_iface_dim table
        // uses wlan mac in ap, instead of ap mac itself
        return "apWlanMac_%d_apWlanName_%s".formatted(this.wlanMac, this.wlanEssid);
    }

    @SneakyThrows
    public String toJson() {
        return JsonUtil.toJson(this);
    }

    @SneakyThrows
    public static ArubaAiWlanTrafficEntity from(String json) {
        if (json == null) return null;
        return JsonUtil.fromJson(json, ArubaAiWlanTrafficEntity.class);
    }
}
