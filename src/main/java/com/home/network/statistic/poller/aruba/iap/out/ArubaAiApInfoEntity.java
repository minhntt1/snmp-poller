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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "aruba_iap_ap_info_stg_ingest")
@NoArgsConstructor
@Builder
@Getter
@Setter // REMEMBER CAREFULLY: IF PERSIST OBJECT TO DB, BUT DOESN’T DECLARE SETTER ON OBJECT, HIBERNATE USES REFLECTION TO CREATE OBJECT -> NULL ATTRIBUTE VALUE
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class ArubaAiApInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime pollTime;
    private Long apMac;
    private String apName;
    private Integer apIp;
    private String apModel;
    private Long apUptimeSeconds;

    // this time (prev) must be always <= o.time (next)
    public int checkUptimeGreater(ArubaAiApInfoEntity o) {
        // when uptime decreases means there is a reboot
        return this.apUptimeSeconds > o.apUptimeSeconds ? 1 : 0;
    }

    public LocalDate obtainWeekDate() {
        // following equivalent to date(poll_time)-interval weekday(poll_time) day in SQL
        // the week is locale independent
        return pollTime.toLocalDate().with(DayOfWeek.MONDAY);
    }

    public String obtainJobApStateKey() {// ap job state key relects coulmns in ap_dim table
        return "apMac_%s_apName_%s".formatted(this.apMac, this.apName);
    }

    public String toJson() {
        return JsonUtil.toJson(this);
    }

    public static ArubaAiApInfoEntity from(String json) {
        if (json == null) return null;
        return JsonUtil.fromJson(json, ArubaAiApInfoEntity.class);
    }
}

