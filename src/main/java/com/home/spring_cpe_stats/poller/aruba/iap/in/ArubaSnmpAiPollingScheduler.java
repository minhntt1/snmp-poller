package com.home.spring_cpe_stats.poller.aruba.iap.in;

import com.home.spring_cpe_stats.poller.aruba.iap.out.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.Snmp;
import org.snmp4j.util.TableUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArubaSnmpAiPollingScheduler {
    private final Snmp snmp;
    private final ArubaAiApInfoRepository arubaAiApInfoRepository;
    private final ArubaAiClientInfoRepository arubaAiClientInfoRepository;
    private final ArubaAiWlanTrafficRepository arubaAiWlanTrafficRepository;
    private final ArubaSnmpAiTarget arubaSnmpAiTarget = new ArubaSnmpAiTarget("udp:192.168.100.253/161");

    @Scheduled(fixedRate = 300_000)  // 5 mins polling for ap info
    @Transactional
    public void pollApInfo() {
        TableUtils tableUtils = new TableUtils(snmp, arubaSnmpAiTarget.obtainPduFactory());

        log.info("start polling ap info");
        List<ArubaAiApInfoEntity> arubaAiApInfoEntities = new ArubaSnmpAiAccessPointRequest()
                .getResponse(arubaSnmpAiTarget, tableUtils)
                .stream()
                .map(ArubaSnmpAiAccessPointResponse::toApInfo)
                .toList();
        log.info("end polling ap info");

        log.info("persisting ap info");
        arubaAiApInfoRepository.saveAll(arubaAiApInfoEntities);
        log.info("completed persisting ap info");
    }

    @Scheduled(fixedRate = 60_000) // 1 mins polling for client info
    @Transactional
    public void pollClientInfo() {
        TableUtils tableUtils = new TableUtils(snmp, arubaSnmpAiTarget.obtainPduFactory());

        log.info("start polling client info");
        List<ArubaAiClientInfoEntity> arubaAiClientInfoEntities = new ArubaSnmpAiClientRequest()
                .getResponse(arubaSnmpAiTarget, tableUtils)
                .stream()
                .map(ArubaSnmpAiClientResponse::toClientInfo)
                .toList();
        log.info("end polling client info");

        log.info("persisting client info");
        arubaAiClientInfoRepository.saveAll(arubaAiClientInfoEntities);
        log.info("completed persisting client info");
    }

    @Scheduled(fixedRate = 300_000) // 5 mins polling for wlan traffic
    @Transactional
    public void pollWlanTraffic() {
        TableUtils tableUtils = new TableUtils(snmp, arubaSnmpAiTarget.obtainPduFactory());

        log.info("start polling wlan traffic");
        List<ArubaAiWlanTrafficEntity> arubaAiWlanTrafficEntities = new ArubaSnmpAiWlanRequest()
                .getResponse(arubaSnmpAiTarget, tableUtils)
                .stream()
                .map(ArubaSnmpAiWlanResponse::toWlanTraffic)
                .toList();
        log.info("end polling wlan traffic");

        log.info("persisting wlan traffic");
        arubaAiWlanTrafficRepository.saveAll(arubaAiWlanTrafficEntities);
        log.info("completed persisting wlan traffic");
    }
}
