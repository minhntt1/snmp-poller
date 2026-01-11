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
        // append dummy tag in response to assure even array index after parsed
        var split = "<dummy>".concat(webResponse).strip().split("<[^>]*>");
        // map array to list
        return Arrays.stream(split).toList();
    }

    // create key containing device mac + mac iface
    // device mac in webresponse, mac iface in listraw
    public List<StatusWifiStationWebDataRaw> parseWebClientResponseToObjects() {
        var listClients = parseWebResponseToList();
        var devices = new ArrayList<StatusWifiStationWebDataRaw>();

        for (int i = 0; i < listClients.size(); i += 25) {    // 25 is max size to skip array to expect next client
            var idx = listClients.get(i + 4);
            var clientMac = listClients.get(i + 8);
            var clientAuth = listClients.get(i + 12);
            var clientAuthMethod = listClients.get(i + 16);
            var webWlanName = listClients.get(i + 20);
            var webIfDescr = listClients.get(i + 24);
            Long snmpPhysAddr = null;

            if (clientMac.isBlank() || webIfDescr.isBlank() || webWlanName.isBlank() || listRaw == null || pollTime == null) {
                // if mandatory fields missing
                continue;
            }

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
