package com.home.network.statistic.poller.igate.gw240.out;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.home.network.statistic.common.util.JsonUtil;
import lombok.*;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusWifiStationWebDataRaw {
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime pollDateTime;
    private String wIdx;
    private String wClientMac;
    private String wAuthenticated;
    private String wAuthMethod;
    private String wWlanESSID;
    private String wPhyName;
    private Long snmpPhysAddr;

    public static StatusWifiStationWebDataRaw from(String json) {
        return JsonUtil.fromJson(json, StatusWifiStationWebDataRaw.class);
    }

    public String toJson() {
        return JsonUtil.toJson(this);
    }

    public long toWClientMacLong() {
        var macString = wClientMac;
        macString = macString.replace(":", "");
        macString = macString.isEmpty() ? "0" : macString;
        return Long.parseLong(macString, 16);
    }

    public String toWWlanESSIDHtmlDecode() {
        return HtmlUtils.htmlUnescape(this.wWlanESSID);
    }

    public String calcDeviceStateKey() {
        return "clientMac_%d_ifDescr_%s_wlanName_%s_physAddress_%d".formatted(toWClientMacLong(), wPhyName, toWWlanESSIDHtmlDecode(), snmpPhysAddr);
    }

    public Object[] calcObjectClientMac() {
        return new Object[] {toWClientMacLong()};
    }
}
