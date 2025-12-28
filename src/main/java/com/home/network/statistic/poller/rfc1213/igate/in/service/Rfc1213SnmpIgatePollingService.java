package com.home.network.statistic.poller.rfc1213.igate.in.service;

import com.home.network.statistic.poller.rfc1213.igate.in.*;
import com.home.network.statistic.poller.rfc1213.igate.out.Rfc1213IgateIftableTrafficEntity;
import com.home.network.statistic.poller.rfc1213.igate.out.Rfc1213IgateIftableTrafficEntityRepo;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.Snmp;
import org.snmp4j.util.TableUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev-executor","prd-executor"})
public class Rfc1213SnmpIgatePollingService {
    private final Snmp snmp;
    private final Rfc1213IgateIftableTrafficEntityRepo rfc1213IgateIftableTrafficEntityRepo;
    //todo: implement way to securely store credentials
    private final List<Rfc1213SnmpIgateTarget> rfc1213SnmpIgateTargets = List.of(
        new Rfc1213SnmpIgateTarget("udp:192.168.100.1/161")
    );

    @Timed(value = "rfc1213.igate.in.polling.iftraffic")
    public void pollIfTraffic() {

        for (Rfc1213SnmpIgateTarget rfc1213SnmpIgateTarget : rfc1213SnmpIgateTargets) {
            TableUtils tableUtils = new TableUtils(snmp, rfc1213SnmpIgateTarget.obtainPduFactory());

            log.info("Polling RFC1213 target {}", rfc1213SnmpIgateTarget);
            List<Rfc1213SnmpIgateIpAddrTableResponse> rfc1213SnmpIgateIpAddrTableResponses = new Rfc1213SnmpIgateIpAddrTableRequest()
                    .getResponse(rfc1213SnmpIgateTarget, tableUtils);
            List<Rfc1213SnmpIgateIfTableResponse> rfc1213SnmpIgateIfTableResponses = new Rfc1213SnmpIgateIfTableRequest()
                    .getResponse(rfc1213SnmpIgateTarget, tableUtils);
            log.info("End polling RFC1213 target {}", rfc1213SnmpIgateTarget);

            List<Rfc1213IgateIftableTrafficEntity> rfc1213IgateIftableTrafficEntities = rfc1213SnmpIgateIfTableResponses
                    .stream()
                    .map(x -> x.toRfc1213IgateIftableTrafficEntity(rfc1213SnmpIgateIpAddrTableResponses))
                    .toList();

            log.info("Persisting target {}", rfc1213SnmpIgateTarget);
            rfc1213IgateIftableTrafficEntityRepo.saveAll(rfc1213IgateIftableTrafficEntities);
            log.info("End persisting target {}", rfc1213SnmpIgateTarget);
        }
    }
}
