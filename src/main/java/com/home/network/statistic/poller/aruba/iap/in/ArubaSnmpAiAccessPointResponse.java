package com.home.network.statistic.poller.aruba.iap.in;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiApInfoEntity;
import com.home.network.statistic.poller.util.VariableBindingUtil;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TableEvent;

import java.time.Clock;

@Slf4j
@ToString
public class ArubaSnmpAiAccessPointResponse extends ArubaSnmpAiResponse {
    private Long aiAPMACAddress;
    private String aiAPName;
    private Integer aiAPIPAddress;
    private String aiAPModelName;
    private Long aiAPUptime;

    public ArubaSnmpAiAccessPointResponse(TableEvent event) {
        super(Clock.systemUTC().millis());

        log.info("table event: {}", event);

        for (VariableBinding column : event.getColumns()) {
            OID oid = column.getOid();

            if (ArubaSnmpAiAccessPointRequest.isOidAiAPMACAddress(oid))
                this.aiAPMACAddress = VariableBindingUtil.parseMACAddress(column);
            else if (ArubaSnmpAiAccessPointRequest.isOidAPName(oid))
                this.aiAPName = column.toValueString();
            else if (ArubaSnmpAiAccessPointRequest.isOidAPIPAddress(oid))
                this.aiAPIPAddress = VariableBindingUtil.parseIPAddress(column);
            else if (ArubaSnmpAiAccessPointRequest.isOidAPModelName(oid))
                this.aiAPModelName = column.toValueString();
            else if (ArubaSnmpAiAccessPointRequest.isOidAPUptime(oid))
                this.aiAPUptime = VariableBindingUtil.parseTimeticks(column);
        }
    }

    public Long toUptimeSeconds() {
        return this.aiAPUptime / 1_000;
    }

    public ArubaAiApInfoEntity toApInfo() {
        log.info("parsed response: {}", this);

        return ArubaAiApInfoEntity.builder()
                .pollTime(this.toCurrentLdt())
                .apMac(this.aiAPMACAddress)
                .apName(this.aiAPName)
                .apIp(this.aiAPIPAddress)
                .apModel(this.aiAPModelName)
                .apUptimeSeconds(this.toUptimeSeconds())
                .build();
    }
}
