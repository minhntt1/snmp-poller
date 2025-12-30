package com.home.network.statistic.poller.igate.gw240.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.home.network.statistic.common.util.JsonUtil;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusWifiStationRaw {
    @JsonIgnore
    private LocalDateTime pollTime;
    private String webResponse;
    private List<SnmpIfTablePhyInfoResponseRaw> listRaw;

    public StatusWifiStationEntity toStatusWifiStationEntity() {
        return StatusWifiStationEntity.builder()
                .pollTime(pollTime)
                .rawData(JsonUtil.toJson(this))
                .build();
    }
}
