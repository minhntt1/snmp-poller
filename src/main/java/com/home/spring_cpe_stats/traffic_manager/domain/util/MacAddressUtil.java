package com.home.spring_cpe_stats.traffic_manager.domain.util;

public class MacAddressUtil {
    public static String normalizeMacAddress(String macAddress) {
        return macAddress.replace(":", "");
    }

    public static Long normalizeMacAddressToLong(String macAddress) {
        return Long.parseLong(normalizeMacAddress(macAddress), 16);
    }
}
