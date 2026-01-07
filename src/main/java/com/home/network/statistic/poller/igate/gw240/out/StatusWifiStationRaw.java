package com.home.network.statistic.poller.igate.gw240.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.home.network.statistic.common.util.JsonUtil;
import lombok.*;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public List<String> parseWebResponseToList() {
        var split = webResponse.split("<[^>]*>");
        // remove blank string from tag
        return Arrays.stream(split).filter(s -> !s.isBlank()).toList();
    }

    // create key containing device mac + mac iface
    // device mac in webresponse, mac iface in listraw
    public List<StatusWifiStationWebDataRaw> parseWebClientResponseToObjects() {
        var listClients = parseWebResponseToList();
        var devices = new ArrayList<StatusWifiStationWebDataRaw>();

        for (int i = 0; i < listClients.size(); i += 6) {
            var idx = listClients.get(i);
            var clientMac = listClients.get(i + 1);
            var clientAuth = listClients.get(i + 2);
            var clientAuthMethod = listClients.get(i + 3);
            var webIfDescr = listClients.get(i + 5);
            var webWlanName = listClients.get(i + 4);
            Long snmpPhysAddr = null;

            for (var snmpData : listRaw)
                if (snmpData.getSnmpIfDescr().equals(webIfDescr)) {
                    snmpPhysAddr = snmpData.getSnmpIfPhysAddress();
                    break;
                }

            devices.add(
                StatusWifiStationWebDataRaw.builder()
                    .pollDateTime(pollTime)
                    .wIdx(idx)
                    .wPhyName(webIfDescr)
                    .wWlanESSID(webWlanName)
                    .wClientMac(clientMac)
                    .wAuthenticated(clientAuth)
                    .wAuthMethod(clientAuthMethod)
                    .snmpPhysAddr(snmpPhysAddr)
                    .build());
        }

        return devices;
    }

    public Set<String> obtainSetOfDeviceState() {
        return parseWebClientResponseToObjects().stream().map(StatusWifiStationWebDataRaw::calcDeviceStateKey).collect(Collectors.toSet());
    }

    public List<Object[]> extractListClientMacForInsert() {
        return parseWebClientResponseToObjects().stream().map(StatusWifiStationWebDataRaw::calcObjectClientMac).toList();
    }

    public List<Object[]> extractListPhysForInsert() {
        // todo: update sql in xml
        var listClients = parseWebClientResponseToObjects();
        var listObjInsert = new ArrayList<Object[]>();

        for (var client : listClients) {
            for (var snmpResp : listRaw)
                if (snmpResp.checkSamePhysName(client))
                    listObjInsert.add(snmpResp.calcObjectForInsertToGwIfaceDim(client));
        }

        return listObjInsert;
    }
}
