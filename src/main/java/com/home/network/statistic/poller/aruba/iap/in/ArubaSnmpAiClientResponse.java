package com.home.network.statistic.poller.aruba.iap.in;

import com.home.network.statistic.poller.aruba.iap.out.ArubaAiClientInfoEntity;
import com.home.network.statistic.poller.util.VariableBindingUtil;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TableEvent;

import java.time.Clock;

@Slf4j
@ToString
public class ArubaSnmpAiClientResponse extends ArubaSnmpAiResponse{
    private Long aiClientMACAddress;
    private Long aiClientWlanMACAddress;
    private Integer aiClientIPAddress;
    private Integer aiClientAPIPAddress;
    private String aiClientName;
    private Integer aiClientSNR;
    private Long aiClientTxDataBytes;
    private Long aiClientRxDataBytes;
    private Long aiClientUptime;

    public ArubaSnmpAiClientResponse(
            TableEvent tableEvent
    ) {
        super(Clock.systemUTC().millis());

        log.info("table event: {}", tableEvent);

        for (VariableBinding column : tableEvent.getColumns()) {
            OID oid = column.getOid();

            if (ArubaSnmpAiClientRequest.isOidAiClientMACAddress(oid))
                this.aiClientMACAddress = VariableBindingUtil.parseMACAddress(column);
            else if (ArubaSnmpAiClientRequest.isOidAiClientWlanMACAddress(oid))
                this.aiClientWlanMACAddress = VariableBindingUtil.parseMACAddress(column);
            else if (ArubaSnmpAiClientRequest.isOidAiClientIPAddress(oid))
                this.aiClientIPAddress = VariableBindingUtil.parseIPAddress(column);
            else if (ArubaSnmpAiClientRequest.isOidAiClientAPIPAddress(oid))
                this.aiClientAPIPAddress = VariableBindingUtil.parseIPAddress(column);
            else if (ArubaSnmpAiClientRequest.isOidAiClientName(oid))
                this.aiClientName = column.toValueString();
            else if (ArubaSnmpAiClientRequest.isOidAiClientSNR(oid))
                this.aiClientSNR = VariableBindingUtil.parseSNR(column);
            else if (ArubaSnmpAiClientRequest.isOidAiClientTxDataBytes(oid))
                this.aiClientTxDataBytes = VariableBindingUtil.parseRxTx(column);
            else if (ArubaSnmpAiClientRequest.isOidAiClientRxDataBytes(oid))
                this.aiClientRxDataBytes = VariableBindingUtil.parseRxTx(column);
            else if (ArubaSnmpAiClientRequest.isOidAiClientUptime(oid))
                this.aiClientUptime = VariableBindingUtil.parseTimeticks(column);
        }
    }

    public Long toClientUptimeSeconds() {
        return this.aiClientUptime / 1_000;
    }

    public ArubaAiClientInfoEntity toClientInfo() {
        log.info("parsed response: {}", this);

        return ArubaAiClientInfoEntity.builder()
                .pollTime(this.toCurrentLdt())
                .deviceMac(this.aiClientMACAddress)
                .deviceWlanMac(this.aiClientWlanMACAddress)
                .deviceIp(this.aiClientIPAddress)
                .deviceApIp(this.aiClientAPIPAddress)
                .deviceName(this.aiClientName)
                .deviceRx(this.aiClientRxDataBytes)
                .deviceTx(this.aiClientTxDataBytes)
                .deviceSnr(this.aiClientSNR)
                .deviceUptimeSeconds(this.toClientUptimeSeconds())
                .build();
    }
}
