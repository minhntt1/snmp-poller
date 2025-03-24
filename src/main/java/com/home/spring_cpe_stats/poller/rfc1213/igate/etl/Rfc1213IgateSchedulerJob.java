package com.home.spring_cpe_stats.poller.rfc1213.igate.etl;

import com.home.spring_cpe_stats.common.model.ListSqlQuery;
import com.home.spring_cpe_stats.poller.rfc1213.igate.out.Rfc1213IgateIftableTrafficEntity;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
@Profile("!local")
public class Rfc1213IgateSchedulerJob implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationContext applicationContext;
    private final ListSqlQuery listSqlQuery;

    public Rfc1213IgateSchedulerJob(JdbcTemplate jdbcTemplate,
                                    ApplicationContext applicationContext,
                                    @Qualifier("rfc1213IgateQuery")
                                    ListSqlQuery listSqlQuery) {
        this.jdbcTemplate = jdbcTemplate;
        this.applicationContext = applicationContext;
        this.listSqlQuery = listSqlQuery;
    }

    public List<Rfc1213IgateIftableTrafficEntity> getUnprocessedList() {
        return jdbcTemplate.query(listSqlQuery.getQueryValue("getUnprocessedList"),
            new BeanPropertyRowMapper<>(Rfc1213IgateIftableTrafficEntity.class)
        );
    }

    public List<Rfc1213IgateIftableTrafficEntity> getProcessedList(String unprocessedIds) {
        return jdbcTemplate.query(
            String.format(listSqlQuery.getQueryValue("getProcessedList"),
                unprocessedIds),
                new BeanPropertyRowMapper<>(Rfc1213IgateIftableTrafficEntity.class
            )
        );
    }

    public void insertIntoGwIfaceDim(String unprocessedIds) {
        // insert into gw_iface_dim
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("insertIntoGwIfaceDim"),
                unprocessedIds
            )
        );
    }

    public void insertIntoDateDim(String unprocessedIds) {
        // insert into date_dim
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("insertIntoDateDim"),
                unprocessedIds
            )
        );
    }

    public void insertIntoTimeDim(String unprocessedIds) {
        // insert into time_dim
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("insertIntoTimeDim"),
                unprocessedIds
            )
        );
    }

    public void insertIntoTimeDimHourNorm(String unprocessedIds) {
        // insert into time_dim
        // insert hour into time dim
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("insertIntoTimeDimHourNorm"),
            unprocessedIds)
        );
    }

    @Transactional
    public void summarizeData(String unprocessedIds,
                              String processedIds,
                              String processedAndUnprocessedIds) {
        log.info("start summarizing data");

        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("getUnprocessedListForUpdate"), unprocessedIds)
        );

        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("getProcessedListForShare"), processedIds)
        );

        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("updateFactTable")
            , processedAndUnprocessedIds)
        );

        log.info("mark records start");

        // once completed, mark all completed
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("markComplete"),
                unprocessedIds
            )
        );

        log.info("mark records completed");

        log.info("end summarizing data");
    }

    @Override
    @Scheduled(fixedRate = 600_000) // 10 min
    public void start() {
        log.info("start");

        List<Rfc1213IgateIftableTrafficEntity> unprocessedList = this.getUnprocessedList();

        if (unprocessedList.isEmpty()) {
            log.info("unprocessed list is empty");
            return;
        }

        String unprocessedIds = Rfc1213IgateIftableTrafficEntity.constructIdString(unprocessedList.parallelStream());

        List<Rfc1213IgateIftableTrafficEntity> processedList = this.getProcessedList(unprocessedIds);

        if (processedList.isEmpty()) {
            processedList.add(Rfc1213IgateIftableTrafficEntity.builder().id(-1L).build());
        }

        String processedIds = Rfc1213IgateIftableTrafficEntity.constructIdString(processedList.parallelStream());
        String processedAndUnprocessedIds = Rfc1213IgateIftableTrafficEntity.constructIdString(
                Stream.concat(unprocessedList.parallelStream(), processedList.parallelStream())
        );

        this.insertIntoGwIfaceDim(unprocessedIds);
        this.insertIntoDateDim(unprocessedIds);
        this.insertIntoTimeDim(unprocessedIds);
        this.insertIntoTimeDimHourNorm(unprocessedIds);

        Rfc1213IgateSchedulerJob ctx = applicationContext.getBean(Rfc1213IgateSchedulerJob.class);

        ctx.summarizeData(unprocessedIds, processedIds, processedAndUnprocessedIds);

        log.info("end");
    }
}
