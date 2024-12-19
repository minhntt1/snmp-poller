package com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.aruba;

import com.home.spring_cpe_stats.traffic_manager.adapter.in.scheduler.snmp.model.CommonSnmpResponse;
import com.home.spring_cpe_stats.traffic_manager.domain.entity.TrafficInfo;
import com.home.spring_cpe_stats.traffic_manager.domain.entity.VendorVo;
import com.home.spring_cpe_stats.traffic_manager.domain.util.IpAddressUtil;
import com.home.spring_cpe_stats.traffic_manager.domain.util.MacAddressUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.snmp4j.smi.VariableBinding;

import java.util.List;

@Getter
@Setter
@ToString
public class ArubaTrafficResponse extends CommonSnmpResponse {
    private static VendorVo vendor = VendorVo.ARUBA_IAP;

    // define for response
    // should be as same as domain model
    private String clientMac;
    private String clientName;
    private String apIp;
    private Integer clientTx;
    private Integer clientRx;

    public ArubaTrafficResponse(
            List<VariableBinding> variableBindings,
            ArubaTrafficRequest arubaTrafficRequest) {
        super(System.currentTimeMillis());

        for (VariableBinding variableBinding : variableBindings) {
            if (variableBinding.getOid().startsWith(arubaTrafficRequest.getOidPduAiClientApIpAddress()))
                this.apIp = variableBinding.getVariable().toString();

            else if (variableBinding.getOid().startsWith(arubaTrafficRequest.getOidPduAiClientMacAddress()))
                this.clientMac = variableBinding.getVariable().toString();

            else if (variableBinding.getOid().startsWith(arubaTrafficRequest.getOidPduAiClientName()))
                this.clientName = variableBinding.getVariable().toString();

            else if (variableBinding.getOid().startsWith(arubaTrafficRequest.getOidPduAiClientRxDataBytes()))
                this.clientRx = Integer.parseInt(variableBinding.getVariable().toString());

            else if (variableBinding.getOid().startsWith(arubaTrafficRequest.getOidPduAiClientTxDataBytes()))
                this.clientTx = Integer.parseInt(variableBinding.getVariable().toString());
        }
    }


    @Override
    public TrafficInfo toTrafficInfo() {
        return TrafficInfo.builder()
                .timestamp(this.getTimestamp())
                .clientMac(MacAddressUtil.normalizeMacAddressToLong(this.clientMac))
                .clientName(this.getClientName())
                .gatewayIpV4(IpAddressUtil.convertIpV4StringToInt32(this.apIp))
                .counterTx(this.clientTx)
                .counterRx(this.clientRx)
                .vendor(vendor)
                .build();
    }
}
