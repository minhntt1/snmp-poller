package com.home.network.statistic.poller.rfc1213.igate.etl.service;

import com.home.network.statistic.common.model.ListSqlQuery;
import com.home.network.statistic.poller.rfc1213.igate.etl.Rfc1213IgateTrafficHourlyCount;
import com.home.network.statistic.poller.rfc1213.igate.out.Rfc1213IgateIftableTrafficEntity;
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
public class Rfc1213IgateService implements BaseService {
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationContext applicationContext;
    private final ListSqlQuery listSqlQuery;

    public Rfc1213IgateService(@Qualifier("appJdbcTemplate") JdbcTemplate jdbcTemplate,
                               ApplicationContext applicationContext,
                               @Qualifier("rfc1213IgateQuery") ListSqlQuery listSqlQuery) {
        this.jdbcTemplate = jdbcTemplate;
        this.applicationContext = applicationContext;
        this.listSqlQuery = listSqlQuery;
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
        // insert into time_dim
        // insert hour into time dim
        jdbcTemplate.execute(listSqlQuery.getQueryValue("insertIntoTimeDimHourNorm"));
    }

    @Transactional(value = "appTx")
    public void summarizeData(JobExecutionContext context) {
        log.info("start summarizing data");

        // get current map of state
        var stateMap = context.getJobDetail().getJobDataMap();

        // copy of state to maintain keys tobe removed
        var copyState = new HashMap<>(stateMap);

        // storing count state
        var mapCountState = new HashMap<Rfc1213IgateTrafficHourlyCount, Rfc1213IgateTrafficHourlyCount>();

        try (var stream = jdbcTemplate.queryForStream(listSqlQuery.getQueryValue("getAllStaging"), new BeanPropertyRowMapper<>(Rfc1213IgateIftableTrafficEntity.class))) {
            for (var it = stream.iterator(); it.hasNext();) {
                var currState = it.next();

                // whether batch processing current entry
                if (!currState.checkUsableEntry())
                    continue;

                var currStateKey = currState.obtainJobStateKey();
                var oldState = Rfc1213IgateIftableTrafficEntity.from(stateMap.getString(currStateKey));

                if (oldState != null) {
                    var count = new Rfc1213IgateTrafficHourlyCount(currState);
                    mapCountState.computeIfAbsent(count, kk -> count).adjustTraffic(oldState, currState);
                }

                stateMap.put(currStateKey, currState.toJson());
                copyState.remove(currStateKey);
            }
        }

        for (var key : copyState.keySet())
            stateMap.remove(key);

        var batchUpdateDb = Rfc1213IgateTrafficHourlyCount.obtainMappedRow(mapCountState);

        if (!batchUpdateDb.isEmpty()) {
            // create temp tbl
            jdbcTemplate.execute(listSqlQuery.getQueryValue("createTempForFact"));

            // update batch data
            jdbcTemplate.batchUpdate(listSqlQuery.getQueryValue("insertTmpFactToTempTable"), batchUpdateDb);

            // update fact table
            jdbcTemplate.execute(listSqlQuery.getQueryValue("updateFactTable"));

            // drop temp tbl
            jdbcTemplate.execute(listSqlQuery.getQueryValue("dropTempForFact"));
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
    @Timed(value = "rfc1213.igate.etl.iftraffic")
    public void start(JobExecutionContext context) {
        log.info("start");

        // normalize
        insertIntoGwIfaceDim();
        insertIntoDateDim();
        insertIntoTimeDim();
        insertIntoTimeDimHourNorm();

        // summarize
        applicationContext.getBean(Rfc1213IgateService.class).summarizeData(context);

        // clean up batch
        cleanUpBatch();

        log.info("end");
    }
}
