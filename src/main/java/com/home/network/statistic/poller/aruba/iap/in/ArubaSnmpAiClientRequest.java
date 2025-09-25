package com.home.network.statistic.poller.aruba.iap.in;

import org.snmp4j.smi.OID;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import java.util.List;

public class ArubaSnmpAiClientRequest extends ArubaSnmpAiRequest<ArubaSnmpAiClientResponse> {
    private static final String aiClientMACAddress = "1.3.6.1.4.1.14823.2.3.3.1.2.4.1.1."; // ADD A DOT AS SUFFIX TO MATCH PREFIX OF TARGET OID
    private static final String aiClientWlanMACAddress = "1.3.6.1.4.1.14823.2.3.3.1.2.4.1.2.";
    private static final String aiClientIPAddress = "1.3.6.1.4.1.14823.2.3.3.1.2.4.1.3.";
    private static final String aiClientAPIPAddress = "1.3.6.1.4.1.14823.2.3.3.1.2.4.1.4.";
    private static final String aiClientName = "1.3.6.1.4.1.14823.2.3.3.1.2.4.1.5.";
    private static final String aiClientSNR = "1.3.6.1.4.1.14823.2.3.3.1.2.4.1.7.";
    private static final String aiClientTxDataBytes = "1.3.6.1.4.1.14823.2.3.3.1.2.4.1.9.";
    private static final String aiClientRxDataBytes = "1.3.6.1.4.1.14823.2.3.3.1.2.4.1.13.";
    private static final String aiClientUptime = "1.3.6.1.4.1.14823.2.3.3.1.2.4.1.16.";

    // NOTE: USE STARTWITH INSTEAD OF EQUALS BECAUSE OF INDEX AFTER OID: 1.3.6.1.4.1.14823.2.3.3.1.2.3.1.1 -> 1.3.6.1.4.1.14823.2.3.3.1.2.1.1.1.216.199.200.194.0.97
    public static boolean isOidAiClientMACAddress(OID oid) {
        return oid.toString().startsWith(aiClientMACAddress);
    }

    public static boolean isOidAiClientWlanMACAddress(OID oid) {
        return oid.toString() .startsWith(aiClientWlanMACAddress);
    }

    public static boolean isOidAiClientIPAddress(OID oid) {
        return oid.toString() .startsWith(aiClientIPAddress);
    }

    public static boolean isOidAiClientAPIPAddress(OID oid) {
        return oid.toString() .startsWith(aiClientAPIPAddress);
    }

    public static boolean isOidAiClientName(OID oid) {
        return oid.toString() .startsWith(aiClientName);
    }

    public static boolean isOidAiClientSNR(OID oid) {
        return oid.toString() .startsWith(aiClientSNR);
    }

    public static boolean isOidAiClientTxDataBytes(OID oid) {
        return oid.toString() .startsWith(aiClientTxDataBytes);
    }

    public static boolean isOidAiClientRxDataBytes(OID oid) {
        return oid.toString() .startsWith(aiClientRxDataBytes);
    }

    public static boolean isOidAiClientUptime(OID oid) {
        return oid.toString() .startsWith(aiClientUptime);
    }

    @Override
    public OID[] getRequestColumns() {
        return new OID[] {
            new OID(aiClientMACAddress),
            new OID(aiClientWlanMACAddress),
            new OID(aiClientIPAddress),
            new OID(aiClientAPIPAddress),
            new OID(aiClientName),
            new OID(aiClientSNR),
            new OID(aiClientTxDataBytes),
            new OID(aiClientRxDataBytes),
            new OID(aiClientUptime)
        };
    }

    @Override
    public List<ArubaSnmpAiClientResponse> getResponse(ArubaSnmpAiTarget target, TableUtils tableUtils) {
        List<TableEvent> tableEvents = tableUtils.getTable(
                target.buildTarget(),
                this.getRequestColumns(),
                null, null
        );

        return tableEvents.stream()
                .map(ArubaSnmpAiClientResponse::new)
                .toList();
    }
}
