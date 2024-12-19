package com.home.spring_cpe_stats.traffic_manager.domain.util;

public class IpAddressUtil {
    public static int convertIpV4StringToInt32(String ipv4String) {
        String[] parts = ipv4String.split("\\.");

        int ip = 0;

        for (String part : parts){
            int partInt = Integer.parseInt(part);
            ip = (ip << 8) + partInt;
        }

        return ip;
    }

}
