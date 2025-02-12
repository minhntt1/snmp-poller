package com.home.spring_cpe_stats.poller.util;

import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;

public class VariableBindingUtil {
    public static Long parseMACAddress(VariableBinding variableBinding) {
        String mac = variableBinding.toValueString();
        mac = mac.replace(":", "");
        mac = mac.replace("-", "");
        mac = mac.isEmpty() ? "0" : mac;
        return Long.parseLong(mac, 16);
    }

    public static int parseIPAddress(VariableBinding variableBinding) {
        String ip = variableBinding.toValueString();
        String[] parts = ip.split("\\.");
        int result = 0;
        for (String part : parts) {
            result = result << 8 | Integer.parseInt(part);
        }
        return result;
    }

    public static Long parseTimeticks(VariableBinding variableBinding) {
        // relative time
        return ((TimeTicks)variableBinding.getVariable())
                .toMilliseconds();    // obtain absolute time
    }

    public static Long parseRxTx(VariableBinding variableBinding) {
        return Long.parseLong(variableBinding.toValueString());
    }

    public static Integer parseSNR(VariableBinding variableBinding) {
        return Integer.parseInt(variableBinding.toValueString());
    }

    public static Integer parseInt(VariableBinding variableBinding) {
        return Integer.parseInt(variableBinding.toValueString());
    }


}
