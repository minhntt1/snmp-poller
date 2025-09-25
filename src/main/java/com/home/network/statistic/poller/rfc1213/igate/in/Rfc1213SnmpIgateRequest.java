package com.home.network.statistic.poller.rfc1213.igate.in;

import org.snmp4j.smi.OID;
import org.snmp4j.util.TableUtils;

import java.util.List;

public abstract class Rfc1213SnmpIgateRequest <T extends Rfc1213SnmpIgateResponse> {
    public abstract OID[] getRequestColumns();
    public abstract List<T> getResponse(Rfc1213SnmpIgateTarget target, TableUtils tableUtils);
}
