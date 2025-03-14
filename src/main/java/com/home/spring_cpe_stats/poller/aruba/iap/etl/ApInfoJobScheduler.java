package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiApInfoEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApInfoJobScheduler implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    @Scheduled(fixedRate = 60_000) // 1 min
    public void start() {
        log.info("start");

        List<ArubaAiApInfoEntity> arubaAiApInfoEntities = jdbcTemplate
            .query("""
                -- 1st step, select needed id and lock them
                with cte1 as (-- select latest one processed row
                    select
                    id
                    from aruba_iap_ap_info_stg
                    where id in (
                    	select
                        id
                        from (
                    		-- for each ap_mac,ap_name,select its latest processed row data (for stateful processing),
                    		-- previous approach (select limit 1) does not work because its only selectlatest 1 record, does not care it belong to which pair of mac,name
                    		select
                    		aiais.id,
                    		row_number() over(partition by aiais.ap_mac,aiais.ap_name order by aiais.poll_time desc,aiais.id desc) as rn
                    		from aruba_iap_ap_info_stg aiais
                    		where aiais.mark=1
                    		for update
                        ) x
                        where x.rn=1
                    )
                ), cte2 as (-- select unprocessed rows, earliest, limit to 200
                    select
                    aiais.id
                    from aruba_iap_ap_info_stg  aiais
                    where aiais.mark=0
                    order by aiais.poll_time asc,aiais.id asc
                    limit 200 
                    for update
                )
                select * from cte1
                union
                select * from cte2;""",
                new BeanPropertyRowMapper<>(ArubaAiApInfoEntity.class)
            );

        log.info("total records: {}", arubaAiApInfoEntities.size());

        long minId = arubaAiApInfoEntities.stream()
                .map(ArubaAiApInfoEntity::getId)
                .min(Long::compareTo)
                .orElse(0L);

        long maxId = arubaAiApInfoEntities.stream()
                .map(ArubaAiApInfoEntity::getId)
                .max(Long::compareTo)
                .orElse(0L);

        log.info("min id: {}, max id: {}", minId, maxId);

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
                    where dd.date_key is null and aiis.id between %d and %d
                )""",
                minId, maxId
            )
        );

        // insert into time_dim using time to sec values of aiis.poll_time
        jdbcTemplate.execute(
            String.format("""
                insert ignore into time_dim(time) (
                    select distinct time_to_sec(time(aiis.poll_time))
                    from aruba_iap_ap_info_stg aiis
                    left join time_dim td on time_to_sec(time(aiis.poll_time)) = td.time
                    where td.time_key is null and aiis.id between %d and %d
                )""",
                minId, maxId
            )
        );

        // insert into ip_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into ip_dim(ipv4) (
                    select distinct aiis.ap_ip
                    from aruba_iap_ap_info_stg aiis
                    left join ip_dim id on aiis.ap_ip = id.ipv4
                    where id.ip_key is null and aiis.id between %d and %d
                )""",
                minId, maxId
            )
        );

        // insert into ap_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into ap_dim(ap_mac,ap_name) (
                    select distinct aiis.ap_mac, aiis.ap_name
                    from aruba_iap_ap_info_stg aiis
                    left join ap_dim ad on aiis.ap_mac = ad.ap_mac and aiis.ap_name = ad.ap_name
                    where ad.ap_key is null and aiis.id between %d and %d
                )""",
                minId, maxId
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
                    where dd.date_key is null and aiais.id between %d and %d
                )""",
                minId, maxId
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
                        aiais.ap_uptime_seconds<lag(aiais.ap_uptime_seconds,1,0) over(partition by dd.date_key,ad.ap_key,id.ip_key order by aiais.poll_time) as cnt
                        from aruba_iap_ap_info_stg aiais
                        join date_dim dd on (date(aiais.poll_time)-interval weekday(aiais.poll_time) day)=dd.date
                        join ap_dim ad on aiais.ap_mac=ad.ap_mac and aiais.ap_name=ad.ap_name
                        join ip_dim id on aiais.ap_ip=id.ipv4
                        where aiais.id between %d and %d
                    ) t
                    group by 1,2,3
                )
                on duplicate key update count_reboot=count_reboot+values(count_reboot)""",
                minId, maxId
            )
        );

        log.info("update fact table completed");

        log.info("mark records start");

        // once completed, mark all completed
        jdbcTemplate.execute(
             String.format("""
                update aruba_iap_ap_info_stg set mark=1 where id between %d and %d""",
                minId, maxId
             )
        );

        log.info("mark records completed");

        log.info("end");
    }
}
