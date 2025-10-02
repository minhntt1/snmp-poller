package com.home.network.statistic.poller.rfc1213.igate.in;

import com.home.network.statistic.poller.rfc1213.igate.out.Rfc1213IgateIftableTrafficEntity;
import com.home.network.statistic.poller.util.VariableBindingUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TableEvent;

import java.time.Clock;
import java.util.List;

@Getter
@RequiredArgsConstructor
enum IfStatus {
    UP("1"),
    DOWN("2"),
    TEST("3");
    private final String value;
    public static IfStatus getIfStatus(int value) {
        if (value == 1)
            return UP;
        if (value == 2)
            return DOWN;
        return TEST;
    }
}

public class Rfc1213SnmpIgateIfTableResponse extends Rfc1213SnmpIgateResponse {
    private Integer ifIndex;
    private String ifDescr;
    private Long ifPhysAddress;
    private IfStatus ifAdminStatus;
    private IfStatus ifOperStatus;
    private Long ifInOctets;
    private Long ifOutOctets;

    public Rfc1213SnmpIgateIfTableResponse(TableEvent event) {
        super(Clock.systemUTC().millis());

        for (VariableBinding variableBinding : event.getColumns()) {
            OID oid = variableBinding.getOid();

            if (Rfc1213SnmpIgateIfTableRequest.isOidIfIndex(oid))
                this.ifIndex = VariableBindingUtil.parseInt(variableBinding);
            else if (Rfc1213SnmpIgateIfTableRequest.isOidIfDescr(oid))
                this.ifDescr = variableBinding.toValueString();
            else if (Rfc1213SnmpIgateIfTableRequest.isOidIfPhysAddress(oid))
                this.ifPhysAddress = VariableBindingUtil.parseMACAddress(variableBinding);
            else if (Rfc1213SnmpIgateIfTableRequest.isOidIfAdminStatus(oid))
                this.ifAdminStatus = IfStatus.getIfStatus(VariableBindingUtil.parseInt(variableBinding));
            else if (Rfc1213SnmpIgateIfTableRequest.isOidIfOperStatus(oid))
                this.ifOperStatus = IfStatus.getIfStatus(VariableBindingUtil.parseInt(variableBinding));
            else if (Rfc1213SnmpIgateIfTableRequest.isOidIfInOctets(oid))
                this.ifInOctets = VariableBindingUtil.parseRxTx(variableBinding);
            else if (Rfc1213SnmpIgateIfTableRequest.isOidIfOutOctets(oid))
                this.ifOutOctets = VariableBindingUtil.parseRxTx(variableBinding);
        }
    }

    public Rfc1213IgateIftableTrafficEntity toRfc1213IgateIftableTrafficEntity(
            List<Rfc1213SnmpIgateIpAddrTableResponse> rfc1213SnmpIgateIpAddrTableResponses
    ) {
        Rfc1213IgateIftableTrafficEntity rfc1213IgateIftableTrafficEntity = new Rfc1213IgateIftableTrafficEntity();
        rfc1213IgateIftableTrafficEntity.setPollTime(this.toCurrentLdt());
        rfc1213IgateIftableTrafficEntity.setIfIndex(this.ifIndex);
        rfc1213IgateIftableTrafficEntity.setIfDescr(this.ifDescr);
        rfc1213IgateIftableTrafficEntity.setIfPhysAddress(this.ifPhysAddress);
        rfc1213IgateIftableTrafficEntity.setIfAdminStatus(this.ifAdminStatus.getValue());
        rfc1213IgateIftableTrafficEntity.setIfOperStatus(this.ifOperStatus.getValue());
        rfc1213IgateIftableTrafficEntity.setIfInOctets(this.ifInOctets);
        rfc1213IgateIftableTrafficEntity.setIfOutOctets(this.ifOutOctets);

        for (Rfc1213SnmpIgateIpAddrTableResponse rfc1213SnmpIgateIpAddrTableResponse : rfc1213SnmpIgateIpAddrTableResponses) {
            if (this.ifIndex.equals(rfc1213SnmpIgateIpAddrTableResponse.getIpAdEntIfIndex())) {
                rfc1213IgateIftableTrafficEntity.setIpAdEntAddr(rfc1213SnmpIgateIpAddrTableResponse.getIpAdEntAddr());
                break;
            }
        }

        return rfc1213IgateIftableTrafficEntity;
    }
}
