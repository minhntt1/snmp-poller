package com.home.network.statistic.poller.aruba.iap.etl.service;

import com.home.network.statistic.common.model.ListSqlQuery;
import com.home.network.statistic.poller.aruba.iap.etl.ApRebootWeeklyCount;
import com.home.network.statistic.poller.aruba.iap.etl.ApTrafficHourlyCount;
import com.home.network.statistic.poller.aruba.iap.out.ArubaAiApInfoEntity;
import com.home.network.statistic.poller.aruba.iap.out.ArubaAiWlanTrafficEntity;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
@Profile({"dev-executor","prd-executor"})
public class ApWlanTrafficService implements BaseService {
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationContext applicationContext;
    private final ListSqlQuery listSqlQuery;

    public ApWlanTrafficService(@Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate,
                                ApplicationContext applicationContext,
                                @Qualifier("apWlanTrafficQuery") ListSqlQuery listSqlQuery) {
        this.jdbcTemplate = jdbcTemplate;
        this.applicationContext = applicationContext;
        this.listSqlQuery = listSqlQuery;
    }

    public void insertIntoApDim() {
        // insert into ap_dim
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoApDim"));
    }

    public void insertIntoGwIfaceDim() {
        // insert into gw_iface_dim
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoGwIfaceDim"));
    }

    public void insertIntoDateDim() {
        // insert into date_dim
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoDateDim"));
    }

    public void insertIntoTimeDim() {
        // insert into time_dim
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoTimeDim"));
    }

    public void insertIntoTimeDimHourNorm() {
        // insert hour into time dim
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoTimeDimHourNorm"));
    }

    @Transactional(value = "appTx")
    public void summarizeData(JobExecutionContext context) {
        log.info("start summarizing data");

        // current job map
        var map = context.getJobDetail().getJobDataMap();

        // copy current job map to get which ones not exist in this iteration
        var copyOfMap = new HashMap<>(map);

        // map reboot cnt
        var mapRebootCnt = new HashMap<ApTrafficHourlyCount, ApTrafficHourlyCount>();

        // stream is guaranteed to return data ordered
        try (var stream = jdbcTemplate.queryForStream(listSqlQuery.getQueryValue("getAllStaging"), new BeanPropertyRowMapper<>(ArubaAiWlanTrafficEntity.class));) {
            for (var it = stream.iterator(); it.hasNext();) {
                // this one map to ap mac, ap name
                var currApState = it.next();
                var apStateKey = currApState.obtainJobApStateKey();

                // get prev state, latest by job data map
                // use entity ArubaAiWlanTrafficEntity for state
                // the latest is reflected by: date, hour, wlan mac, wlan essid
                var prevApState = ArubaAiWlanTrafficEntity.from(map.getString(apStateKey));

                // the purpose is to count transmission bytes per hour per day
                // prev state is null, then total transmit curr-prev = 0
                // traffic sum by day, hour, wlan mac, wlan ssid
                if (prevApState != null) {
                    var kv = new ApTrafficHourlyCount(currApState);
                    mapRebootCnt.computeIfAbsent(kv, k -> kv).updateTraffic(prevApState, currApState);
                }

                // update latest state to original map (must convert to json string)
                map.put(apStateKey, currApState.toJson());

                // remove the exist key in copyOfMap
                copyOfMap.remove(apStateKey);
            }
        }

        // remove all from map keys in copyOfMap
        for (var k : copyOfMap.keySet()) {
            map.remove(k);
        }

        // obtain ap reboot cnt update query
        String queryUpdateRebootCnt = ApTrafficHourlyCount.obtainSqlValues(mapRebootCnt);

        // update to fact table
        if (!queryUpdateRebootCnt.isBlank())
            jdbcTemplate.execute(listSqlQuery.getQueryValue("updateFactTable").formatted(queryUpdateRebootCnt));

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
        // normalize data
        insertIntoApDim();
        insertIntoGwIfaceDim();
        insertIntoDateDim();
        insertIntoTimeDim();
        insertIntoTimeDimHourNorm();

        // summarize data, call in transaction
        applicationContext.getBean(ApWlanTrafficService.class).summarizeData(context);

        // clean up batch
        cleanUpBatch();
    }
}
