package com.home.network.statistic.poller.rfc1213.igate.in;

import org.snmp4j.smi.OID;

public class SnmpIfTablePhyInfoRequest extends Rfc1213SnmpIgateIfTableRequest {
    @Override
    public OID[] getRequestColumns() {
        return new OID[]{
            new OID(ifDescr),
            new OID(ifPhysAddress)
        };
    }
}
