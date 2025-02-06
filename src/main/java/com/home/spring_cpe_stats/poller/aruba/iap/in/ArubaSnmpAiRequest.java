package com.home.spring_cpe_stats.poller.aruba.iap.in;

import org.snmp4j.smi.OID;
import org.snmp4j.util.TableUtils;

import java.util.List;

public abstract class ArubaSnmpAiRequest <T extends ArubaSnmpAiResponse> {
    public abstract OID[] getRequestColumns();
    public abstract List<T> getResponse(ArubaSnmpAiTarget target, TableUtils tableUtils);
}
