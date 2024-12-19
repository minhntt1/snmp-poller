package com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.aruba;

import com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.CommonSnmpRequest;
import com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.CommonSnmpResponse;
import com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.SnmpConnSettings;
import lombok.Getter;
import lombok.Setter;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Component("arubaTrafficRequest")
@ConfigurationProperties("aruba.snmp")
public class ArubaTrafficRequest extends CommonSnmpRequest {
    // should be one controller for each atuba ap version
    // common aruba controllers with same version may share same snmp oids
    private List<SnmpConnSettings> arubaControllers = new ArrayList<>();

    // vs request thi set cac pdu option, snmp ver
    // applies for client only

    // define pdu repetition for each request
    private int pduRepetitions;

    // define start table string to stop polling snmp records
    private String startTableOidForStop;

    // get mac client, client name, ap ip, client tx data bytes, rx data bytes
    private String pduAiClientMacAddress; // mac client
    private String pduAiClientApIpAddress; // ap ip
    private String pduAiClientName;   // client name
    private String pduAiClientTxDataBytes; // client tx = ap tx = client download
    private String pduAiClientRxDataBytes; // client rx = ap rx = client upload

    // get start oid object
    public OID getStartOid() {
        return new OID(startTableOidForStop);
    }

    public OID getOidPduAiClientMacAddress() {
        return new OID(pduAiClientMacAddress);
    }

    public OID getOidPduAiClientTxDataBytes() {
        return new OID(pduAiClientTxDataBytes);
    }

    public OID getOidPduAiClientRxDataBytes() {
        return new OID(pduAiClientRxDataBytes);
    }

    public OID getOidPduAiClientApIpAddress() {
        return new OID(pduAiClientApIpAddress);
    }

    public OID getOidPduAiClientName() {
        return new OID(pduAiClientName);
    }

    //define get list targets
    public List<CommunityTarget<Address>> getCommunityTargets() {
        List<CommunityTarget<Address>> communityTargets = new ArrayList<>();

        for (SnmpConnSettings arubaController : arubaControllers) {
            CommunityTarget<Address> communityTarget = new CommunityTarget<>();
            communityTarget.setTimeout(arubaController.getTargetTimeout());
            communityTarget.setRetries(arubaController.getTargetRetries());
            communityTarget.setAddress(GenericAddress.parse(arubaController.getTarget()));
            communityTarget.setCommunity(new OctetString(arubaController.getCommunity()));
            communityTarget.setVersion(arubaController.getVersion());

            communityTargets.add(communityTarget);
        }

        return communityTargets;
    }

    // define get pdu
    public PDU getPDU() {
        List<VariableBinding> variableBindings = Arrays.asList(
                new VariableBinding(new OID(this.pduAiClientMacAddress)),
                new VariableBinding(new OID(this.pduAiClientApIpAddress)),
                new VariableBinding(new OID(this.pduAiClientName)),
                new VariableBinding(new OID(this.pduAiClientTxDataBytes)),
                new VariableBinding(new OID(this.pduAiClientRxDataBytes))
        );

        PDU pdu = new PDU(PDU.GETNEXT, variableBindings);
        pdu.setMaxRepetitions(this.pduRepetitions);

        return pdu;
    }

    // define polling
    // list poll result response (arb traffic response)
    public List<CommonSnmpResponse> getTrafficResponse(Snmp snmp) throws IOException {
        // define start oid for stop
        OID startOid = this.getStartOid();

        // list target
        List<CommunityTarget<Address>> communityTargets = this.getCommunityTargets();

        // list response result
        List<CommonSnmpResponse> arubaTrafficResponses = new ArrayList<>();

        for (CommunityTarget<Address> communityTarget : communityTargets) {
            ResponseEvent<Address> addressResponseEvent;

            PDU pdu = this.getPDU();

            while ((addressResponseEvent = snmp.send(pdu, communityTarget)) != null) {
                PDU responsePdu = addressResponseEvent.getResponse();

                if (responsePdu == null)
                    break;

                List<VariableBinding> variableBindings = responsePdu.getAll();

                if (variableBindings == null ||
                        variableBindings.isEmpty() ||
                        !variableBindings.get(0).getOid().startsWith(startOid))
                    break;

                // map to response
                arubaTrafficResponses.add(
                        new ArubaTrafficResponse(variableBindings, this)
                );

                // get next for current variablebindings
                pdu.setVariableBindings(variableBindings);
            }
        }

        return arubaTrafficResponses;
    }
}
