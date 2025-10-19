package com.home.network.statistic.poller.aruba.iap.etl.service;

import com.home.network.statistic.common.model.ListSqlQuery;
import com.home.network.statistic.poller.aruba.iap.etl.ApRebootWeeklyCount;
import com.home.network.statistic.poller.aruba.iap.out.ArubaAiApInfoEntity;
import io.micrometer.core.annotation.Timed;
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

@Service
@Slf4j
@Profile({"dev-executor","prd-executor"})
public class ApInfoService implements BaseService {
    private final JdbcTemplate jdbcTemplate;
    private final ListSqlQuery listSqlQuery;
    private final ApplicationContext applicationContext;

    public ApInfoService(@Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate,
                         @Qualifier("apInfoQuery") ListSqlQuery listSqlQuery,
                         ApplicationContext applicationContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.listSqlQuery = listSqlQuery;
        this.applicationContext = applicationContext;
    }

    private void insertIntoWeekDateDim() {
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoWeekDateDim"));
    }

    private void insertIntoDateDim() {
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoDateDim"));
    }

    private void insertIntoTimeDim() {
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoTimeDim"));
    }

    private void insertIntoIpDim() {
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoIpDim"));
    }

    private void insertIntoApDim() {
        // insert into ap_dim
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoApDim"));
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

    @Transactional(value = "appTx")
    void summarizeData(JobExecutionContext context) {
        log.info("start summarizing data");

        // current job map
        var map = context.getJobDetail().getJobDataMap();

        // copy current job map to get which ones not exist in this iteration
        var copyOfMap = new HashMap<>(map);

        // map reboot cnt
        var mapRebootCnt = new HashMap<ApRebootWeeklyCount, ApRebootWeeklyCount>();

        // stream is guaranteed to return data ordered
        try (var stream = jdbcTemplate.queryForStream(listSqlQuery.getQueryValue("getAllStaging"), new BeanPropertyRowMapper<>(ArubaAiApInfoEntity.class));) {
            for (var it = stream.iterator(); it.hasNext();) {
                // this one map to ap mac, ap name
                var currApState = it.next();
                var apStateKey = currApState.obtainJobApStateKey();

                // get prev state, latest by job data map
                // use entity arubaapinforentity for state
                var prevApState = ArubaAiApInfoEntity.from(map.getString(apStateKey));

                // the purpose is to count reboot, in other words, count all that have prev uptime > curr uptime
                // if prev state is null, then count = 0
                // reboot count sum count by week, ap key, ip key - on application, then embed the result values to sql query
                if (prevApState != null) {
                    var kv = new ApRebootWeeklyCount(currApState);
                    mapRebootCnt.computeIfAbsent(kv, k -> kv).adjustRebootCnt(prevApState, currApState);
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

        // obtain batched data
        var batch = ApRebootWeeklyCount.obtainMappedRow(mapRebootCnt);

        // update to fact table
        if (!batch.isEmpty()) {
            // create temp tbl
            jdbcTemplate.execute(listSqlQuery.getQueryValue("createTempForUpdateFactTable"));

            // update batch data
            jdbcTemplate.batchUpdate(listSqlQuery.getQueryValue("insertTmpFactToTempTableUpdateFactTable"), batch);

            // update fact table
            jdbcTemplate.execute(listSqlQuery.getQueryValue("updateFactTable"));

            // drop temp tbl
            jdbcTemplate.execute(listSqlQuery.getQueryValue("dropTempForUpdateFactTable"));
        }

        log.info("end summarizing data");
    }

    @Override
    @Timed(value = "aruba.iap.etl.ap.info")
    public void start(JobExecutionContext context) {
        log.info("start");

        // ap info service
        // first normalize all queries in staging to dimension
        insertIntoDateDim();
        insertIntoWeekDateDim();
        insertIntoTimeDim();
        insertIntoIpDim();
        insertIntoApDim();

        // summarize inside a transaction
        applicationContext.getBean(this.getClass()).summarizeData(context);

        // after summarizing done, delete old table, swap ingest table and batch table to next process
        cleanUpBatch();

        log.info("end");
    }
}
