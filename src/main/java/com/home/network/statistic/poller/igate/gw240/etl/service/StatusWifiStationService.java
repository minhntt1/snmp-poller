package com.home.network.statistic.poller.igate.gw240.etl.service;

import com.home.network.statistic.common.model.ListSqlQuery;
import com.home.network.statistic.poller.igate.gw240.etl.ClientWlanConnectEvent;
import com.home.network.statistic.poller.igate.gw240.out.StatusWifiStationEntity;
import com.home.network.statistic.poller.igate.gw240.out.StatusWifiStationWebDataRaw;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Profile({"dev-executor","prd-executor"})
public class StatusWifiStationService implements BaseService {
    private final JdbcTemplate jdbcTemplate;
    private final ListSqlQuery listSqlQuery;
    private final ApplicationContext applicationContext;

    @Autowired
    public StatusWifiStationService(@Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate,
                                    @Qualifier("igate240StatusWifiStation") ListSqlQuery listSqlQuery,
                                    ApplicationContext applicationContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.listSqlQuery = listSqlQuery;
        this.applicationContext = applicationContext;
    }

    public void addUndefinedIpDim() {
        jdbcTemplate.execute(listSqlQuery.getQueryValue("addUndefinedIpDim"));
    }

    public void insertIntoDateDim() {
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoDateDim"));
    }

    public void insertIntoTimeDim() {
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoTimeDim"));
    }

    // put inside one transaction to stay in one connection
    @Transactional(value = "appTx")
    public void insertIntoDeviceDimAndGwIfaceDim() {
        log.info("start normalizing to device dim and gw iface dim");

        List<Object[]> clientMacs = new ArrayList<>();

        List<Object[]> gwIfacePhys = new ArrayList<>();

        // select from stg ingest
        try (var stream = jdbcTemplate
                .queryForStream(
                        listSqlQuery.getQueryValue("getAllStaging"),
                        new BeanPropertyRowMapper<>(StatusWifiStationEntity.class))) {
            for (var it = stream.iterator(); it.hasNext(); ) {
                // process ingest
                var currentRaw = it.next().obtainStatusWifiStationRaw();

                // get list extracted macs for insertion
                clientMacs.addAll(currentRaw.extractListClientMacForInsert());

                // get list of phys for insertion
                gwIfacePhys.addAll(currentRaw.extractListPhysForInsert());
            }
        }

        if (!clientMacs.isEmpty()) {
            // create temp device dim
            jdbcTemplate.execute(listSqlQuery.getQueryValue("createTempTableForDeviceDimNormalize"));

            // insert into temp device dim
            jdbcTemplate.batchUpdate(listSqlQuery.getQueryValue("insertIntoTempTableForDeviceDimNormalize"), clientMacs);

            // normalize from temp to actual device dim
            jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoDeviceDim"));

            // drop temp device dim
            jdbcTemplate.execute(listSqlQuery.getQueryValue("dropTempTableForDeviceDimNormalize"));
        }

        if (!gwIfacePhys.isEmpty()) {
            // create temp gw iface dim
            jdbcTemplate.execute(listSqlQuery.getQueryValue("createTempTableForGwIfaceDimNormalize"));

            // insert into temp gw iface dim
            jdbcTemplate.batchUpdate(listSqlQuery.getQueryValue("insertIntoTempTableForGwIfaceDimNormalize"), gwIfacePhys);

            // normalize from temp to actual gw_iface_dim
            jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoGwIfaceDim"));

            // drop temp gw iface dim
            jdbcTemplate.execute(listSqlQuery.getQueryValue("dropTempTableForGwIfaceDimNormalize"));
        }

        log.info("end normalizing to device dim and gw iface dim");
    }

    @Transactional(value = "appTx")
    public void summarizeData(JobExecutionContext context) {
        log.info("start summarizing data");

        // current job map
        var stateMap = context.getJobDetail().getJobDataMap();

        // list of connect events
        var events = new ArrayList<ClientWlanConnectEvent>();

        // select from stg ingest
        try (var stream = jdbcTemplate
                .queryForStream(
                        listSqlQuery.getQueryValue("getAllStaging"),
                        new BeanPropertyRowMapper<>(StatusWifiStationEntity.class))) {
            for (var it = stream.iterator(); it.hasNext(); ) {
                // process ingest
                var currentRaw = it.next();

                // current poll time of record in current batch
                LocalDateTime pollTime = currentRaw.getPollTime();

                // get object for raw in current record
                var currentRawObj = currentRaw.obtainStatusWifiStationRaw();

                // for each event in current batch
                // check if map has state
                var devices = currentRawObj.parseWebClientResponseToObjects();
                var deviceSet = currentRawObj.obtainSetOfDeviceState();

                // loop thru map, check if
                // there is device in current batch that doesn't exist in state map
                // that consider the device in map is disconnected
                for (var stateIt = stateMap.entrySet().iterator(); it.hasNext(); ) {
                    var state = stateIt.next();
                    var stateVal = StatusWifiStationWebDataRaw.from(state.getValue().toString());

                    // if current batch doesn't have state key in state map
                    if (!deviceSet.contains(state.getKey())) {
                        // add disconnect event
                        events.add(new ClientWlanConnectEvent(stateVal, pollTime));
                        // remove state from map
                        stateIt.remove();
                    }
                }

                // for each device state, check in map has prev state
                for (var device : devices) {
                    var deviceStateKey = device.calcDeviceStateKey();

                    // if map has no state key
                    if (!stateMap.containsKey(deviceStateKey)) {
                        // add connect event
                        events.add(new ClientWlanConnectEvent(device));
                        // add state to map
                        stateMap.put(deviceStateKey, device.toJson());
                    }
                }
            }
        }

        var eventsForInsert = ClientWlanConnectEvent.toObjectsForInsert(events);

        if (!eventsForInsert.isEmpty()) {
            jdbcTemplate.execute(listSqlQuery.getQueryValue("createTempForUpdateFactTableDeviceWlanConnections"));

            jdbcTemplate.batchUpdate(listSqlQuery.getQueryValue("insertTmpFactToTempTableUpdateFactTableDeviceWlanConnections"), eventsForInsert);

            jdbcTemplate.execute(listSqlQuery.getQueryValue("updateFactTableDeviceWlanConnections"));

            jdbcTemplate.execute(listSqlQuery.getQueryValue("dropTempForUpdateFactTableDeviceWlanConnections"));
        }

        log.info("end summarizing data");
    }

    private void cleanUpBatch() {
        // copy all staging data to archive
        jdbcTemplate.execute(listSqlQuery.getQueryValue("copyStgToArchive"));

        // drop current staging (batch processing table)
        jdbcTemplate.execute(listSqlQuery.getQueryValue("dropCurrentStg"));

        // create new batch process table
        jdbcTemplate.execute(listSqlQuery.getQueryValue("createNewStg"));

        // flip new batch process table with ingest table for further processing
        jdbcTemplate.execute(listSqlQuery.getQueryValue("moveStgToIngest"));
    }

    @Override
    public void start(JobExecutionContext context) {
        log.info("start");

        // normalize
        addUndefinedIpDim();
        insertIntoDateDim();
        insertIntoTimeDim();
        applicationContext.getBean(StatusWifiStationService.class).insertIntoDeviceDimAndGwIfaceDim();

        // summarize
        applicationContext.getBean(StatusWifiStationService.class).summarizeData(context);

        // clean up batch
        cleanUpBatch();

        log.info("end");
    }
}
