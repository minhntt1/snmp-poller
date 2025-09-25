package com.home.network.statistic.poller.snmp;

import lombok.AllArgsConstructor;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;

@AllArgsConstructor
public class BaseTarget {
    private Integer timeout = 2500;
    private Integer retries = 2;
    private String community = "public";
    private Integer version = 1;
    private String address;

    public BaseTarget(Integer timeout, Integer retries, String address) {
        this.timeout = timeout;
        this.retries = retries;
        this.address = address;
    }

    public BaseTarget(Integer timeout, String address) {
        this.timeout = timeout;
        this.address = address;
    }

    public BaseTarget(String address) {
        this.address = address;
    }

    public CommunityTarget<Address> buildTarget() {
        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setVersion(version);
        target.setTimeout(timeout);
        target.setRetries(retries);
        target.setCommunity(new OctetString(community));
        target.setAddress(GenericAddress.parse(this.address));
        return target;
    }

    public PDUFactory obtainPduFactory() {
        DefaultPDUFactory pduFactory = new DefaultPDUFactory(PDU.GETBULK);
        pduFactory.setMaxRepetitions(-1);
        return pduFactory;
    }

    @Override
    public String toString() {
        return "BaseTarget{" +
                "address='" + address + '\'' +
                '}';
    }
}
