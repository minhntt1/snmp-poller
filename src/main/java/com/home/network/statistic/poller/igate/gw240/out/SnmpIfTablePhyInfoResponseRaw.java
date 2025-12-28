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
}
