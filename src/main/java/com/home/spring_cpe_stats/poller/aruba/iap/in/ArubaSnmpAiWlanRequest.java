package com.home.spring_cpe_stats.poller.aruba.iap.in;

import org.snmp4j.smi.OID;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import java.util.List;

public class ArubaSnmpAiWlanRequest extends ArubaSnmpAiRequest<ArubaSnmpAiWlanResponse> {
    private static final String aiWlanAPMACAddress = "1.3.6.1.4.1.14823.2.3.3.1.2.3.1.1.";
    private static final String aiWlanESSID = "1.3.6.1.4.1.14823.2.3.3.1.2.3.1.3.";
    private static final String aiWlanMACAddress = "1.3.6.1.4.1.14823.2.3.3.1.2.3.1.4.";
    private static final String aiWlanTxDataBytes = "1.3.6.1.4.1.14823.2.3.3.1.2.3.1.7.";
    private static final String aiWlanRxDataBytes = "1.3.6.1.4.1.14823.2.3.3.1.2.3.1.10.";

    // NOTE: USE STARTWITH INSTEAD OF EQUALS BECAUSE OF INDEX AFTER OID: 1.3.6.1.4.1.14823.2.3.3.1.2.3.1.1 -> 1.3.6.1.4.1.14823.2.3.3.1.2.1.1.1.216.199.200.194.0.97
    public static boolean isOidAiWlanAPMACAddress(OID oid) {
        return oid.toString() .startsWith(aiWlanAPMACAddress);
    }

    public static boolean isOidAiWlanESSID(OID oid) {
        return oid.toString() .startsWith(aiWlanESSID);
    }

    public static boolean isOidAiWlanMACAddress(OID oid) {
        return oid.toString() .startsWith(aiWlanMACAddress);
    }

    public static boolean isOidAiWlanTxDataBytes(OID oid) {
        return oid.toString() .startsWith(aiWlanTxDataBytes);
    }

    public static boolean isOidAiWlanRxDataBytes(OID oid) {
        return oid.toString() .startsWith(aiWlanRxDataBytes);
    }

    @Override
    public OID[] getRequestColumns() {
        return new OID[]{
            new OID(aiWlanAPMACAddress),
            new OID(aiWlanESSID),
            new OID(aiWlanMACAddress),
            new OID(aiWlanTxDataBytes),
            new OID(aiWlanRxDataBytes)
        };
    }

    @Override
    public List<ArubaSnmpAiWlanResponse> getResponse(ArubaSnmpAiTarget target, TableUtils tableUtils) {
        List<TableEvent> tableEvents = tableUtils
                .getTable(target.buildTarget(),
                        this.getRequestColumns(),
                        null,null);

        return tableEvents.stream()
                .map(ArubaSnmpAiWlanResponse::new)
                .toList();
    }
}
