package com.home.network.statistic.poller.aruba.iap.etl.service;

import com.home.network.statistic.common.model.ListSqlQuery;
import com.home.network.statistic.poller.aruba.iap.out.ArubaAiApInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

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

    private void insertIntoDateDim(String unprocessedIds) {
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("insertIntoDateDim"), unprocessedIds)
        );
    }

    private void insertIntoTimeDim(String unprocessedIds) {
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("insertIntoTimeDim"), unprocessedIds)
        );
    }

    private void insertIntoIpDim(String unprocessedIds) {
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("insertIntoIpDim"), unprocessedIds)
        );
    }

    private void insertIntoApDim(String unprocessedIds) {
        // insert into ap_dim
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("insertIntoApDim"), unprocessedIds)
        );
    }

    private List<ArubaAiApInfoEntity> getListUnprocessed() {
        return jdbcTemplate
            .query(listSqlQuery.getQueryValue("getListUnprocessed"),
                new BeanPropertyRowMapper<>(ArubaAiApInfoEntity.class)
            );
    }

    private List<ArubaAiApInfoEntity> getListProcessed(String unprocessedIds) {
        return jdbcTemplate
            .query(String.format(listSqlQuery.getQueryValue("getListProcessed"), unprocessedIds),
            new BeanPropertyRowMapper<>(ArubaAiApInfoEntity.class)
        );
    }

    @Transactional(value = "appTx")
    void summarizeData(String unprocessedIds, String processedIds, String processedAndUnprocessed) {
        log.info("start summarizing data");

        // not allowing other transactions processing these rows
        // if not query for update, others multi transactions can insert into ap_reboot_cnt_per_week_fact -> wrong result
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("getListUnprocessedForUpdate"), unprocessedIds)
        );

        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("getListProcessedForShare"), processedIds)
        );

        // insert/update into fact table: ap_reboot_cnt_per_week_fact
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("updateFactTable"), processedAndUnprocessed)
        );

        log.info("mark records start");

        // once completed, mark all completed
        jdbcTemplate.execute(
            String.format(listSqlQuery.getQueryValue("markComplete"), unprocessedIds)
        );

        log.info("mark records completed");

        log.info("end summarizing data");
    }

    @Override
//    @Scheduled(fixedRate = 600_000) // 10 min
    public void start() {
        log.info("start");

        List<ArubaAiApInfoEntity> listUnprocessed = getListUnprocessed();

        if (listUnprocessed.isEmpty()) {
            log.info("no records found");
            return;
        }

        String unprocessedIds = ArubaAiApInfoEntity.constructIdString(listUnprocessed.stream());

        List<ArubaAiApInfoEntity> listProcessed = this.getListProcessed(unprocessedIds);

        if (listProcessed.isEmpty()) {
            listProcessed.add(ArubaAiApInfoEntity.builder().id(-1L).build());
        }

        String processedIds = ArubaAiApInfoEntity.constructIdString(listProcessed.parallelStream());

        String unprocessedAndProcessedIds = ArubaAiApInfoEntity.constructIdString(
                Stream.concat(listProcessed.parallelStream(), listUnprocessed.parallelStream())
        );

        this.insertIntoDateDim(unprocessedIds);
        this.insertIntoTimeDim(unprocessedIds);
        this.insertIntoIpDim(unprocessedIds);
        this.insertIntoApDim(unprocessedIds);

        ApInfoService ctx = this.applicationContext.getBean(this.getClass());

        ctx.summarizeData(unprocessedIds, processedIds, unprocessedAndProcessedIds);

        log.info("end");
    }
}
