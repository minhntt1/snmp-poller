package com.home.network.statistic.poller.igate.gw240.in.service;

import com.home.network.statistic.poller.igate.gw240.in.WebResponse;
import com.home.network.statistic.poller.igate.gw240.in.WebUICredentials;
import com.home.network.statistic.poller.igate.gw240.out.StatusWifiStationEntity;
import com.home.network.statistic.poller.igate.gw240.out.StatusWifiStationRepo;
import com.home.network.statistic.poller.rfc1213.igate.in.Rfc1213SnmpIgateIfTableResponse;
import com.home.network.statistic.poller.rfc1213.igate.in.Rfc1213SnmpIgateTarget;
import com.home.network.statistic.poller.rfc1213.igate.in.SnmpIfTablePhyInfoRequest;
import lombok.NoArgsConstructor;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev-executor","prd-executor"})
public class Igate240StatusWifiStationService {
    private final Snmp snmp;
    private final RestClient restClient;
    private final Igate240WebAuthService igate240WebAuthService;
    private final ExecutorService virtualThreadPool;
    private final StatusWifiStationRepo statusWifiStationRepo;
    //todo: implement way to securely store credentials
    private final Set<WebUICredentials> hostCred = Set.of(new WebUICredentials("admin", "12345678aA@", "192.168.100.248"));
    private final Map<String, Rfc1213SnmpIgateTarget> rfc1213SnmpIgateTargets = Map.of("192.168.100.248", new Rfc1213SnmpIgateTarget("udp:192.168.100.248/161"));

    @Autowired
    public Igate240StatusWifiStationService(
            @Qualifier("virtualThreadPool") ExecutorService executorService,
            Snmp snmp,
            RestClient restClient,
            Igate240WebAuthService igate240WebAuthService,
            StatusWifiStationRepo statusWifiStationRepo) {
        this.snmp = snmp;
        this.restClient = restClient;
        this.igate240WebAuthService = igate240WebAuthService;
        this.statusWifiStationRepo = statusWifiStationRepo;
        this.virtualThreadPool = executorService;
    }

    @SneakyThrows
    public void pollStatusWifiStation() {
        for (var host : hostCred) {
            log.info("get status wifi station for :{}", host);

            // retrieve auth + rest api info
            var info = igate240WebAuthService.obtainInfo(host, false);

            // query snmp client to get table info
            var snmpTg = rfc1213SnmpIgateTargets.get(host.getHost());

            // define tableutils
            TableUtils tableUtils = new TableUtils(snmp, snmpTg.obtainPduFactory());

            // return future for parallel calling, return header as well to reauth later
            /*
            2 cases:
            reauth always request to get token multime to get rid modem bug
            poll status call api using token from auth sevice if get error immediately call auth svc get new toen
            if err is client, recall auth
            if server err report log
             */
            Future<ResponseEntity<String>> webFuture = virtualThreadPool.submit(() -> restClient.get().uri(info.obtainHostUrlWifiStation()).headers(info::addHeader).retrieve().toEntity(String.class));

            // limit the columns to get in Rfc1213SnmpIgateIfTableRequest in the response by creating SnmpIfTablePhyInfoRequest, to reduce latency
            // return future for parallel calling
            Future<List<Rfc1213SnmpIgateIfTableResponse>> snmpFuture = virtualThreadPool.submit(() -> new SnmpIfTablePhyInfoRequest().getResponse(snmpTg, tableUtils));

            WebResponse webResponse = null;

            // if return body fail do reauth
            if (webFuture.get().getStatusCode().is4xxClientError()) {
                igate240WebAuthService.obtainInfo(host, true);
                webResponse = new WebResponse(restClient.get().uri(info.obtainHostUrlWifiStation()).headers(info::addHeader).retrieve().body(String.class));
            }
            // if server errr
            else if (webFuture.get().getStatusCode().is5xxServerError()) {
                log.error("fetch from server modem err : {}", host);
                continue;
            }
            // if succc
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
