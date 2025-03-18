package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiApInfoEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!local")
public class ApInfoJobScheduler implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    @Scheduled(fixedRate = 600_000) // 10 min
    public void start() {
        log.info("start");

        List<ArubaAiApInfoEntity> listUnprocessed = jdbcTemplate
            .query("""
                select
                aiais.id
                from aruba_iap_ap_info_stg  aiais
                where aiais.mark=0
                order by aiais.poll_time,aiais.id
                limit 10000
                for update""",
                new BeanPropertyRowMapper<>(ArubaAiApInfoEntity.class));

        if (listUnprocessed.isEmpty()) {
            log.info("no aruba ai ap info record found");
            return;
        }

        String unprocessedIds =
                listUnprocessed
                .stream()
                .map(ArubaAiApInfoEntity::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(",","(",")"));

        List<ArubaAiApInfoEntity> listProcessed = jdbcTemplate
            .query(String.format("""
                with cte as (
                    select
                    distinct
                    (date(aiais.poll_time)-interval weekday(aiais.poll_time) day) as date,
                    aiais.ap_mac,
                    aiais.ap_name,
                    aiais.ap_ip
                    from aruba_iap_ap_info_stg  aiais
                    where aiais.id in %s
                )
                select
                id
                from (
                    select
                    aiais.id,
                    row_number() over(partition by date(aiais.poll_time)-interval weekday(aiais.poll_time) day,aiais.ap_mac,aiais.ap_name,aiais.ap_ip order by aiais.poll_time desc,aiais.id desc) as rn
                    from (
                        select
                        *
                        from aruba_iap_ap_info_stg
                        where mark=1
                        order by id desc
                        limit 100 -- last 4 hrs
                        for share
                    ) aiais
                    where (date(aiais.poll_time)-interval weekday(aiais.poll_time) day,aiais.ap_mac,aiais.ap_name,aiais.ap_ip)
                    in (select * from cte)
                ) x
                where x.rn=1
                """,
                unprocessedIds),
                new BeanPropertyRowMapper<>(ArubaAiApInfoEntity.class));

        String processedAndUnprocessed =
                Stream.concat(listProcessed.stream(), listUnprocessed.stream())
                .map(ArubaAiApInfoEntity::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(",","(",")"));

        log.info("unprocessed ids: {}", unprocessedIds);

        log.info("processedAndUnprocessed: {}", processedAndUnprocessed);

        /*
         * ap_dim
         * time_dim
         * date_dim
         * ip_dim
         */

        log.info("insert into dim table start");

        // insert into date_dim using date values of aiis.poll_tme
        jdbcTemplate.execute(
            String.format("""
                insert ignore into date_dim(date) (
                    select distinct date(aiis.poll_time)
                    from aruba_iap_ap_info_stg aiis
                    left join date_dim dd on date(aiis.poll_time) = dd.date
                    where dd.date_key is null and aiis.id in %s
                )""",
                unprocessedIds
            )
        );

        // insert into time_dim using time to sec values of aiis.poll_time
        jdbcTemplate.execute(
            String.format("""
                insert ignore into time_dim(time) (
                    select distinct time_to_sec(time(aiis.poll_time))
                    from aruba_iap_ap_info_stg aiis
                    left join time_dim td on time_to_sec(time(aiis.poll_time)) = td.time
                    where td.time_key is null and aiis.id in %s
                )""",
                unprocessedIds
            )
        );

        // insert into ip_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into ip_dim(ipv4) (
                    select distinct aiis.ap_ip
                    from aruba_iap_ap_info_stg aiis
                    left join ip_dim id on aiis.ap_ip = id.ipv4
                    where id.ip_key is null and aiis.id in %s
                )""",
                unprocessedIds
            )
        );

        // insert into ap_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into ap_dim(ap_mac,ap_name) (
                    select distinct aiis.ap_mac, aiis.ap_name
                    from aruba_iap_ap_info_stg aiis
                    left join ap_dim ad on aiis.ap_mac = ad.ap_mac and aiis.ap_name = ad.ap_name
                    where ad.ap_key is null and aiis.id in %s
                )""",
                unprocessedIds
            )
        );

        log.info("insert into dim table completed");

        jdbcTemplate.execute(
            String.format("""
                -- 3rd step: insert week date of current processing rows into date dim table
                insert ignore into date_dim(date) (
                    select
                    distinct
                    date(aiais.poll_time)-interval weekday(aiais.poll_time) day
                    from aruba_iap_ap_info_stg aiais
                    left join date_dim dd on (date(aiais.poll_time)-interval weekday(aiais.poll_time) day) = dd.date
                    where dd.date_key is null and aiais.id in %s
                )""",
                unprocessedIds
            )
        );

        log.info("update fact table start");

        // insert/update into fact table: ap_reboot_cnt_per_week_fact
        jdbcTemplate.execute(
            String.format("""
                -- 4 th step: update fact table
                insert into ap_reboot_cnt_per_week_fact(date_key, ap_key, ip_key, count_reboot) (
                    select
                    date_key,
                    ap_key,
                    ip_key,
                    sum(cnt) as cnt
                    from (
                        select
                        dd.date_key,
                        ad.ap_key,
                        id.ip_key,
                        aiais.ap_uptime_seconds<lag(aiais.ap_uptime_seconds,1,0) over(partition by dd.date_key,ad.ap_key,id.ip_key order by aiais.poll_time,aiais.id) as cnt
                        from aruba_iap_ap_info_stg aiais
                        join date_dim dd on (date(aiais.poll_time)-interval weekday(aiais.poll_time) day)=dd.date
                        join ap_dim ad on aiais.ap_mac=ad.ap_mac and aiais.ap_name=ad.ap_name
                        join ip_dim id on aiais.ap_ip=id.ipv4
                        where aiais.id in %s
                    ) t
                    group by 1,2,3
                )
                on duplicate key update count_reboot=count_reboot+values(count_reboot)""",
                processedAndUnprocessed
            )
        );

        log.info("update fact table completed");

        log.info("mark records start");

        // once completed, mark all completed
        jdbcTemplate.execute(
             String.format("""
                update aruba_iap_ap_info_stg set mark=1 where id in %s""",
                unprocessedIds
             )
        );

        log.info("mark records completed");

        log.info("end");
    }
}
