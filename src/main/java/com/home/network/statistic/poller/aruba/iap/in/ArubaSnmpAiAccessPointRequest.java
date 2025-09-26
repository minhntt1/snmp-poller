package com.home.network.statistic.poller.aruba.iap.in;

import org.snmp4j.smi.OID;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import java.util.List;

public class ArubaSnmpAiAccessPointRequest extends ArubaSnmpAiRequest<ArubaSnmpAiAccessPointResponse> {
    // define required column oids
    private static final String aiAPMACAddress = "1.3.6.1.4.1.14823.2.3.3.1.2.1.1.1.";
    private static final String aiAPName = "1.3.6.1.4.1.14823.2.3.3.1.2.1.1.2.";
    private static final String aiAPIPAddress = "1.3.6.1.4.1.14823.2.3.3.1.2.1.1.3.";
    private static final String aiAPModelName = "1.3.6.1.4.1.14823.2.3.3.1.2.1.1.6.";
    private static final String aiAPUptime = "1.3.6.1.4.1.14823.2.3.3.1.2.1.1.9.";

    // NOTE: USE STARTWITH INSTEAD OF EQUALS BECAUSE OF INDEX AFTER OID: 1.3.6.1.4.1.14823.2.3.3.1.2.3.1.1 -> 1.3.6.1.4.1.14823.2.3.3.1.2.1.1.1.216.199.200.194.0.97
    public static boolean isOidAiAPMACAddress(OID oid) {
        return oid.toString() .startsWith(aiAPMACAddress);
    }

    public static boolean isOidAPName(OID oid) {
        return oid.toString() .startsWith(aiAPName);
    }

    public static boolean isOidAPIPAddress(OID oid) {
        return oid.toString() .startsWith(aiAPIPAddress);
    }

    public static boolean isOidAPModelName(OID oid) {
        return oid.toString() .startsWith(aiAPModelName);
    }

    public static boolean isOidAPUptime(OID oid) {
        return oid.toString() .startsWith(aiAPUptime);
    }

    @Override
    public OID[] getRequestColumns() {
        return new OID[] {
            new OID(ArubaSnmpAiAccessPointRequest.aiAPMACAddress),
            new OID(ArubaSnmpAiAccessPointRequest.aiAPName),
            new OID(ArubaSnmpAiAccessPointRequest.aiAPIPAddress),
            new OID(ArubaSnmpAiAccessPointRequest.aiAPModelName),
            new OID(ArubaSnmpAiAccessPointRequest.aiAPUptime)
        };
    }

    @Override
    public List<ArubaSnmpAiAccessPointResponse> getResponse(
            ArubaSnmpAiTarget target,
            TableUtils tableUtils) {
        List<TableEvent> tableEvents = tableUtils.getTable(
                target.buildTarget(),
                this.getRequestColumns(),
                null, null
        );

        return tableEvents.stream()
                .map(ArubaSnmpAiAccessPointResponse::new)
                .toList();
    }
}
