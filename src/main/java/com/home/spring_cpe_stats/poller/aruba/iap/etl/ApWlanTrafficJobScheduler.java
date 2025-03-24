package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import com.home.spring_cpe_stats.common.model.ListSqlQuery;
import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiWlanTrafficEntity;
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
public class ApWlanTrafficJobScheduler implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationContext applicationContext;
    private final ListSqlQuery listSqlQuery;

    public ApWlanTrafficJobScheduler(JdbcTemplate jdbcTemplate,
                                     ApplicationContext applicationContext,
                                     @Qualifier("apWlanTrafficQuery")
                                     ListSqlQuery listSqlQuery) {
        this.jdbcTemplate = jdbcTemplate;
        this.applicationContext = applicationContext;
        this.listSqlQuery = listSqlQuery;
    }

    public List<ArubaAiWlanTrafficEntity> getListUnprocessed() {
        return jdbcTemplate
            .query(listSqlQuery.getQueryValue("getListUnprocessed"),
                new BeanPropertyRowMapper<>(ArubaAiWlanTrafficEntity.class)
            );
    }

    public List<ArubaAiWlanTrafficEntity> getListProcessed(String unprocessedIds) {
        return jdbcTemplate
            .query(String.format(
                    listSqlQuery.getQueryValue("getListProcessed"),
                        unprocessedIds),
                    new BeanPropertyRowMapper<>(ArubaAiWlanTrafficEntity.class)
            );
    }

    public void insertIntoApDim(String unprocessedIds) {
        // insert into ap_dim
        jdbcTemplate.execute(
            String.format(
                listSqlQuery.getQueryValue("insertIntoApDim"),
                unprocessedIds
            )
        );
    }

    public void insertIntoGwIfaceDim(String unprocessedIds) {
        // insert into gw_iface_dim
        jdbcTemplate.execute(
            String.format(
                    listSqlQuery.getQueryValue("insertIntoGwIfaceDim"),
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
            unprocessedIds)
        );
    }

    public void insertIntoTimeDimHourNorm(String unprocessedIds) {
        // insert hour into time dim
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("insertIntoTimeDimHourNorm"),
            unprocessedIds)
        );
    }

    @Transactional
    public void summarizeData(String unprocessedIds,
                              String processedIds,
                              String unprocessedAndProcessedIds) {
        log.info("start summarizing data");

        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("getListUnprocessedForUpdate"),
                unprocessedIds
            )
        );

        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("getListProcessedForShare"), processedIds)
        );

        // insert into fact table
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("updateFactTable"),
                unprocessedAndProcessedIds
            )
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

        List<ArubaAiWlanTrafficEntity> listUnprocessed = this.getListUnprocessed();

        if (listUnprocessed.isEmpty()) {
            log.info("listUnprocessed is empty");
            return;
        }

        String unprocessedIds = ArubaAiWlanTrafficEntity.constructIdString(listUnprocessed.parallelStream());

        List<ArubaAiWlanTrafficEntity> listProcessed = this.getListProcessed(unprocessedIds);

        if (listProcessed.isEmpty()) {
            listProcessed.add(ArubaAiWlanTrafficEntity.builder().id(-1L).build());
        }

        String processedIds = ArubaAiWlanTrafficEntity.constructIdString(listProcessed.parallelStream());
        String unprocessedAndProcessedIds = ArubaAiWlanTrafficEntity.constructIdString(
                Stream.concat(listUnprocessed.parallelStream(), listProcessed.parallelStream())
        );

        this.insertIntoApDim(unprocessedIds);
        this.insertIntoGwIfaceDim(unprocessedIds);
        this.insertIntoDateDim(unprocessedIds);
        this.insertIntoTimeDim(unprocessedIds);
        this.insertIntoTimeDimHourNorm(unprocessedIds);

        ApWlanTrafficJobScheduler ctx = applicationContext.getBean(ApWlanTrafficJobScheduler.class);

        ctx.summarizeData(unprocessedIds, processedIds, unprocessedAndProcessedIds);

        log.info("end");
    }
}
