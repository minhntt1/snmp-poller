package com.home.spring_cpe_stats.poller.rfc1213.igate.in;

import com.home.spring_cpe_stats.poller.snmp.BaseTarget;

public class Rfc1213SnmpIgateTarget extends BaseTarget {
    public Rfc1213SnmpIgateTarget(String address) {
        super(
                60_000,
                5,
                address);
    }
}
