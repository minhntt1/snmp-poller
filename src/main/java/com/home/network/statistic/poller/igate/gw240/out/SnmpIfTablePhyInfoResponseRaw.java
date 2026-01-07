package com.home.network.statistic.poller.igate.gw240.out;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SnmpIfTablePhyInfoResponseRaw {
    private String snmpIfDescr;
    private Long snmpIfPhysAddress;

    public boolean checkSamePhysName(StatusWifiStationWebDataRaw webDataRaw) {
        return snmpIfDescr.equals(webDataRaw.getWPhyName());
    }

    public Object[] calcObjectForInsertToGwIfaceDim(StatusWifiStationWebDataRaw webDataRaw) {
        return new Object[] {snmpIfDescr, snmpIfPhysAddress, webDataRaw.toWWlanESSIDHtmlDecode()};
    }
}
