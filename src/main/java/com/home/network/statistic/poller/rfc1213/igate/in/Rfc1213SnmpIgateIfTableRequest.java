package com.home.network.statistic.poller.rfc1213.igate.in;

import org.snmp4j.smi.OID;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import java.util.List;

public class Rfc1213SnmpIgateIfTableRequest extends Rfc1213SnmpIgateRequest<Rfc1213SnmpIgateIfTableResponse> {
    private static final String ifIndex = "1.3.6.1.2.1.2.2.1.1.";
    private static final String ifDescr = "1.3.6.1.2.1.2.2.1.2.";
    private static final String ifPhysAddress = "1.3.6.1.2.1.2.2.1.6.";
    private static final String ifAdminStatus = "1.3.6.1.2.1.2.2.1.7.";
    private static final String ifOperStatus = "1.3.6.1.2.1.2.2.1.8.";
    private static final String ifInOctets = "1.3.6.1.2.1.2.2.1.10.";
    private static final String ifOutOctets = "1.3.6.1.2.1.2.2.1.16.";

    public static boolean isOidIfIndex(OID oid) {
        return oid.toString().startsWith(ifIndex);
    }

    public static boolean isOidIfDescr(OID oid) {
        return oid.toString().startsWith(ifDescr);
    }

    public static boolean isOidIfPhysAddress(OID oid) {
        return oid.toString().startsWith(ifPhysAddress);
    }

    public static boolean isOidIfAdminStatus(OID oid) {
        return oid.toString().startsWith(ifAdminStatus);
    }

    public static boolean isOidIfOperStatus(OID oid) {
        return oid.toString().startsWith(ifOperStatus);
    }

    public static boolean isOidIfInOctets(OID oid) {
        return oid.toString().startsWith(ifInOctets);
    }

    public static boolean isOidIfOutOctets(OID oid) {
        return oid.toString().startsWith(ifOutOctets);
    }

    @Override
    public OID[] getRequestColumns() {
        return new OID[]{
            new OID(ifIndex),
            new OID(ifDescr),
            new OID(ifPhysAddress),
            new OID(ifAdminStatus),
            new OID(ifOperStatus),
            new OID(ifInOctets),
            new OID(ifOutOctets)
        };
    }

    @Override
    public List<Rfc1213SnmpIgateIfTableResponse> getResponse(Rfc1213SnmpIgateTarget target, TableUtils tableUtils) {
        List<TableEvent> tableEvents = tableUtils.getTable(
                target.buildTarget(),
                this.getRequestColumns(),
                null,null
        );

        return tableEvents.stream()
                .map(Rfc1213SnmpIgateIfTableResponse::new)
                .toList();
    }
}
