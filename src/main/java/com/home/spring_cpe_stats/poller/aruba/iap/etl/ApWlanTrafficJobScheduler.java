package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiApInfoEntity;
import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiWlanTrafficEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.batch.BatchTaskExecutor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApWlanTrafficJobScheduler implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    @Scheduled(fixedRate = 3_600_000) // 1 hour
    public void start() {
        log.info("start");

        List<ArubaAiWlanTrafficEntity> arubaAiWlanTrafficEntities = jdbcTemplate
            .query("""
                -- just select mark = 0 because don't care value in the past
                select
                aiwts.id
                from aruba_iap_wlan_traffic_stg aiwts
                where aiwts.mark=0
                order by aiwts.poll_time,aiwts.id
                limit 1000
                for update
                """,
                new BeanPropertyRowMapper<>(ArubaAiWlanTrafficEntity.class)
            );

        log.info("total records: {}", arubaAiWlanTrafficEntities.size());

        long minId = arubaAiWlanTrafficEntities.stream()
            .map(ArubaAiWlanTrafficEntity::getId)
            .min(Long::compareTo)
            .orElse(0L);

        long maxId = arubaAiWlanTrafficEntities.stream()
            .map(ArubaAiWlanTrafficEntity::getId)
            .max(Long::compareTo)
            .orElse(0L);

        log.info("min id: {}, max id: {}", minId, maxId);

        /*
        * ap_dim
        * gw_iface_dim
        * date_dim
        * time_dim
        * */

        log.info("insert into dim table start");

        // insert into ap_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into ap_dim(ap_mac,ap_name) (
                    select
                    distinct
                    aiwts.wlan_ap_mac,
                    ''  -- by default, current stg table does not have ap_name
                    from aruba_iap_wlan_traffic_stg aiwts
                    left join ap_dim ad on aiwts.wlan_ap_mac=ad.ap_mac
                    where ad.ap_key is null
                    and aiwts.id between %d and %d
                )""",
                minId, maxId
            )
        );

        // insert into gw_iface_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into gw_iface_dim(iface_mac,iface_name) (
                    select
                    distinct
                    aiwts.wlan_mac,
                    aiwts.wlan_essid
                    from aruba_iap_wlan_traffic_stg aiwts
                    left join gw_iface_dim gid on aiwts.wlan_mac=gid.iface_mac
                    and aiwts.wlan_essid=gid.iface_name
                    where gid.iface_key is null
                    and aiwts.id between %d and %d
                )""",
                minId,maxId
            )
        );

        // insert into date_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into date_dim(date) (
                    select
                    distinct
                    date(aiwts.poll_time)
                    from aruba_iap_wlan_traffic_stg aiwts
                    left join date_dim dd on date(aiwts.poll_time)=dd.date
                    where dd.date_key is null and aiwts.id between %d and %d
                );""",
                minId, maxId
            )
        );

        // insert into time_dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into time_dim(time) (
                    select
                    distinct
                    time_to_sec(time(aiwts.poll_time))
                    from aruba_iap_wlan_traffic_stg aiwts
                    left join time_dim td on time_to_sec(time(aiwts.poll_time))=td.time
                    where td.time_key is null and aiwts.id between %d and %d
                )""",
                minId, maxId)
        );

        // insert hour into time dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into time_dim(time) (
                    select
                    distinct time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00'))
                    from aruba_iap_wlan_traffic_stg aiwts
                    left join time_dim td on time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00'))=td.time
                    where td.time_key is null and aiwts.id between %d and %d
                )""",
            minId, maxId)
        );

        log.info("insert into dim table completed");

        log.info("update fact table start");

        // insert into fact table
        jdbcTemplate.execute(
            String.format("""
                insert into iface_traffic_by_hour_fact(date_key,time_key,iface_key,transmission_bytes) (
                    select
                    *
                    from (
                        select
                        x.date_key,
                        x.time_key,
                        x.iface_key,
                        sum(transmission_bytes) as transmission_bytes_val
                        from (
                            select
                            dd.date_key,
                            td.time_key,
                            gid.iface_key,
                            -- consider case when previous tx/rx value is > current value (when counter reset to 0),
                            -- then accept cuurent value
                            -- if there is no previous value, then consider previous is current value
                            aiwts.wlan_tx - if(
                                lag(aiwts.wlan_tx,1,aiwts.wlan_tx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time)
                                >
                                aiwts.wlan_tx,
                                0,
                                lag(aiwts.wlan_tx,1,aiwts.wlan_tx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time)
                            )
                            +
                            aiwts.wlan_rx - if(
                                lag(aiwts.wlan_rx,1,aiwts.wlan_rx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time)
                                >
                                aiwts.wlan_rx,
                                0,
                                lag(aiwts.wlan_rx,1,aiwts.wlan_rx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time)
                            )
                            as transmission_bytes
                            from aruba_iap_wlan_traffic_stg aiwts
                            join date_dim dd on date(aiwts.poll_time)=dd.date
                            join time_dim td on time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00'))=td.time
                            join gw_iface_dim gid on aiwts.wlan_mac=gid.iface_mac and aiwts.wlan_essid=gid.iface_name
                            where aiwts.id between %d and %d
                        ) x
                        group by 1,2,3
                    ) tmp
                )
                on duplicate key update transmission_bytes=transmission_bytes+transmission_bytes_val""",
                minId, maxId
            )
        );

        log.info("update fact table completed");

        log.info("mark records start");

        // once completed, mark all completed
        jdbcTemplate.execute(
            String.format("""
                update aruba_iap_wlan_traffic_stg set mark=1 where id between %d and %d""",
                minId, maxId
            )
        );

        log.info("mark records completed");

        log.info("end");
    }
}
