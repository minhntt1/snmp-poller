package com.home.spring_cpe_stats.poller.rfc1213.igate.in;

import org.snmp4j.smi.OID;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import java.util.List;
import java.util.stream.Collectors;

public class Rfc1213SnmpIgateIpAddrTableRequest extends Rfc1213SnmpIgateRequest<Rfc1213SnmpIgateIpAddrTableResponse> {
    private static final String ipAdEntAddr = "1.3.6.1.2.1.4.20.1.1.";
    private static final String ipAdEntIfIndex = "1.3.6.1.2.1.4.20.1.2.";

    public static boolean isOidIpAdEntAddr(OID oid) {
        return oid.toString().startsWith(ipAdEntAddr);
    }

    public static boolean isOidIpAdEntIfIndex(OID oid) {
        return oid.toString().startsWith(ipAdEntIfIndex);
    }

    @Override
    public OID[] getRequestColumns() {
        return new OID[] {
            new OID(ipAdEntAddr),
            new OID(ipAdEntIfIndex),
        };
    }

    @Override
    public List<Rfc1213SnmpIgateIpAddrTableResponse> getResponse(Rfc1213SnmpIgateTarget target, TableUtils tableUtils) {
        List<TableEvent> tableEvents = tableUtils.getTable(
                target.buildTarget(),
                this.getRequestColumns(),
                null,null
        );
        return tableEvents.stream()
                .map(Rfc1213SnmpIgateIpAddrTableResponse::new)
                .toList();
    }
}
