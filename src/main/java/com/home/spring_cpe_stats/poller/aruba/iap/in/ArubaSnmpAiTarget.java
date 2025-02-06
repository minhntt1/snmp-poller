package com.home.spring_cpe_stats.poller.aruba.iap.in;

import lombok.RequiredArgsConstructor;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;

@RequiredArgsConstructor
public class ArubaSnmpAiTarget {
    private static final Integer timeout = 2500;
    private static final Integer retries = 2;
    private static final String community = "public";
    private static final Integer version = 1;
    private static final Integer pduRep = 10;

    private final String address;

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
}
