package com.home.spring_cpe_stats;

import com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.aruba.ArubaTrafficRequest;
import org.junit.jupiter.api.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
class SpringCpeStatsApplicationTests {
    @Autowired
    private Snmp snmp;

    @Autowired
    private ArubaTrafficRequest arubaTrafficRequest;

    @Test
    void testGetSnmp() throws IOException {
        OID startOid = arubaTrafficRequest.getStartOid();
        List<CommunityTarget<Address>> list = arubaTrafficRequest.getCommunityTargets();

        for (CommunityTarget<Address> communityTarget : list) {
            ResponseEvent<Address> addressResponseEvent;

            PDU pdu = arubaTrafficRequest.getPDU();

            while ((addressResponseEvent = snmp.send(pdu, communityTarget)) != null) {
                PDU resp = addressResponseEvent.getResponse();

                if (resp == null)
                    continue;

                List<VariableBinding> variableBindings = resp.getAll();

                if (variableBindings == null ||
                    variableBindings.isEmpty() ||
                    !variableBindings.get(0).getOid().startsWith(startOid))
                    break;

                for (VariableBinding variableBinding : variableBindings) {
                    System.out.println(variableBinding.getOid() + " " + variableBinding.getVariable());
                }

                pdu.setVariableBindings(variableBindings);
            }
        }
    }

    @Test
    void contextLoads() {
    }

}
