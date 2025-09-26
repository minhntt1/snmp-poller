package com.home.network.statistic.poller.rfc1213.igate.in;

import com.home.network.statistic.poller.util.VariableBindingUtil;
import lombok.Getter;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TableEvent;

import java.time.Clock;

@Getter
public class Rfc1213SnmpIgateIpAddrTableResponse extends Rfc1213SnmpIgateResponse {
    private Integer ipAdEntAddr;
    private Integer ipAdEntIfIndex;

    public Rfc1213SnmpIgateIpAddrTableResponse(TableEvent tableEvent) {
        super(Clock.systemUTC().millis());

        for (VariableBinding variableBinding : tableEvent.getColumns()) {
            OID oid = variableBinding.getOid();
            if (Rfc1213SnmpIgateIpAddrTableRequest.isOidIpAdEntAddr(oid))
                this.ipAdEntAddr = VariableBindingUtil.parseIPAddress(variableBinding);
            else if(Rfc1213SnmpIgateIpAddrTableRequest.isOidIpAdEntIfIndex(oid))
                this.ipAdEntIfIndex = VariableBindingUtil.parseInt(variableBinding);
        }
    }
}
