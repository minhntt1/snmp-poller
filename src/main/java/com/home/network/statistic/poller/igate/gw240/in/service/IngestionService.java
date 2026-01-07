package com.home.network.statistic.poller.igate.gw240.in.service;

import com.home.network.statistic.poller.igate.gw240.in.WebRequestInfo;
import com.home.network.statistic.poller.igate.gw240.in.WebResponse;
import com.home.network.statistic.poller.igate.gw240.in.WebUICredentials;
import com.home.network.statistic.poller.igate.gw240.out.StatusWifiStationRepo;
import com.home.network.statistic.poller.rfc1213.igate.in.Rfc1213SnmpIgateIfTableResponse;
import com.home.network.statistic.poller.rfc1213.igate.in.Rfc1213SnmpIgateTarget;
import com.home.network.statistic.poller.rfc1213.igate.in.SnmpIfTablePhyInfoRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.Snmp;
import org.snmp4j.util.TableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev-executor","prd-executor"})
public class IngestionService {
    private final Snmp snmp;
    private final RestClient restClient;
    private final WebAuthService webAuthService;
    private final ExecutorService virtualThreadPool;
    private final StatusWifiStationRepo statusWifiStationRepo;
    //todo: implement way to securely store credentials
    private final Set<WebUICredentials> hostCred = Set.of(new WebUICredentials("admin", "12345678aA@", "192.168.100.248"));
    private final Map<String, Rfc1213SnmpIgateTarget> rfc1213SnmpIgateTargets = Map.of("192.168.100.248", new Rfc1213SnmpIgateTarget("udp:192.168.100.248/161"));

    @Autowired
    public IngestionService(
            @Qualifier("virtualThreadPool") ExecutorService executorService,
            Snmp snmp,
            RestClient restClient,
            WebAuthService webAuthService,
            StatusWifiStationRepo statusWifiStationRepo) {
        this.snmp = snmp;
        this.restClient = restClient;
        this.webAuthService = webAuthService;
        this.statusWifiStationRepo = statusWifiStationRepo;
        this.virtualThreadPool = executorService;
    }

    private ResponseEntity<String> getStatusWifiStationRouter(WebRequestInfo info) {
        return restClient.get().uri(info.obtainHostUrlWifiStation()).headers(info::addHeader).retrieve().toEntity(String.class);
    }

    @SneakyThrows
    public void pollStatusWifiStation() {
        for (var host : hostCred) {
            log.info("get status wifi station for :{}", host);

            // retrieve auth + rest api info
            var info = webAuthService.obtainInfo(host, false);

            // query snmp client to get table info
            var snmpTg = rfc1213SnmpIgateTargets.get(host.getHost());

            // define tableutils
            TableUtils tableUtils = new TableUtils(snmp, snmpTg.obtainPduFactory());

            // return future for parallel calling, return header as well to reauth later
            var webFuture = virtualThreadPool.submit(() -> getStatusWifiStationRouter(info));

            // limit the columns to get in Rfc1213SnmpIgateIfTableRequest in the response by creating SnmpIfTablePhyInfoRequest, to reduce latency
            // return future for parallel calling
            var snmpFuture = virtualThreadPool.submit(() -> new SnmpIfTablePhyInfoRequest().getResponse(snmpTg, tableUtils));

            WebResponse webResponse = null;

            // reauth always request to get token multime to get rid modem bug
            // if return body fail do reauth
            if (webFuture.get().getStatusCode().is4xxClientError()) {
                webAuthService.obtainInfo(host, true);
                webResponse = new WebResponse(getStatusWifiStationRouter(info).getBody());
            }
            // if server errr
            else if (webFuture.get().getStatusCode().is5xxServerError()) {
                log.error("fetch from server modem err : {}", host);
                continue;
            }
            // if response success
            else {
                log.info("get from web success {}", host);
                webResponse = new WebResponse(webFuture.get().getBody());
            }

            List<Rfc1213SnmpIgateIfTableResponse> snmpResponse = null;
            try {
                snmpResponse = snmpFuture.get();
            } catch (Exception e) {
                log.error("error when fetching snmp data ", e);
                continue;
            }

            // save web and snmp data in a single raw
            // how to serialize
            // -> save data in another raw class in out package
            // then serialize the class to raw in entity class
            // package dependency follows: in -> out
            // etl -> out
            var statusWifiStationEntity = webResponse.toStatusWifiStationEntity(snmpResponse);

            // save entity including web response + snmp response to db
            statusWifiStationRepo.save(statusWifiStationEntity);

            log.info("done status wifi station for :{}", host);
        }
    }
}
