package com.home.network.statistic.poller.rfc1213.igate.in;

import com.home.network.statistic.poller.snmp.BaseTarget;

public class Rfc1213SnmpIgateTarget extends BaseTarget {
    public Rfc1213SnmpIgateTarget(String address) {
        super(
                60_000,
                5,
                address);
    }
}
