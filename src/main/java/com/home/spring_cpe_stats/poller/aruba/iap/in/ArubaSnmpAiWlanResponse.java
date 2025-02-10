package com.home.spring_cpe_stats.poller.aruba.iap.in;

import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiWlanTrafficEntity;
import com.home.spring_cpe_stats.poller.util.VariableBindingUtil;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TableEvent;

import java.time.Clock;

@Slf4j
@ToString
public class ArubaSnmpAiWlanResponse extends ArubaSnmpAiResponse {
    private Long aiWlanAPMACAddress;
    private String aiWlanESSID;
    private Long aiWlanMACAddress;
    private Long aiWlanTxDataBytes;
    private Long aiWlanRxDataBytes;

    public ArubaSnmpAiWlanResponse(
            TableEvent tableEvent
    ) {
        super(Clock.systemUTC().millis());

        log.info("table event: {}", tableEvent);

        for (VariableBinding column : tableEvent.getColumns()) {
            OID oid = column.getOid();

            if (ArubaSnmpAiWlanRequest.isOidAiWlanAPMACAddress(oid))
                this.aiWlanAPMACAddress = VariableBindingUtil.parseMACAddress(column);
            else if (ArubaSnmpAiWlanRequest.isOidAiWlanESSID(oid))
                this.aiWlanESSID = column.toValueString();
            else if (ArubaSnmpAiWlanRequest.isOidAiWlanMACAddress(oid))
                this.aiWlanMACAddress = VariableBindingUtil.parseMACAddress(column);
            else if (ArubaSnmpAiWlanRequest.isOidAiWlanTxDataBytes(oid))
                this.aiWlanTxDataBytes = VariableBindingUtil.parseRxTx(column);
            else if (ArubaSnmpAiWlanRequest.isOidAiWlanRxDataBytes(oid))
                this.aiWlanRxDataBytes = VariableBindingUtil.parseRxTx(column);
        }
    }

    public ArubaAiWlanTrafficEntity toWlanTraffic() {
        log.info("parsed response: {}", this);

        return ArubaAiWlanTrafficEntity.builder()
                .pollTime(this.toUTC7DateTime())
                .wlanApMac(this.aiWlanAPMACAddress)
                .wlanEssid(this.aiWlanESSID)
                .wlanMac(this.aiWlanMACAddress)
                .wlanRx(this.aiWlanRxDataBytes)
                .wlanTx(this.aiWlanTxDataBytes)
                .mark(0)
                .build();
    }
}
