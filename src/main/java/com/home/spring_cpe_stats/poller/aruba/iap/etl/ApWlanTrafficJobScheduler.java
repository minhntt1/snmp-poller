package com.home.spring_cpe_stats.poller.aruba.iap.etl;

import com.home.spring_cpe_stats.poller.aruba.iap.out.ArubaAiWlanTrafficEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!local")
public class ApWlanTrafficJobScheduler implements BaseScheduler {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    @Scheduled(fixedRate = 600_000) // 10 min
    public void start() {
        log.info("start");

        List<ArubaAiWlanTrafficEntity> unprocessedList = jdbcTemplate
            .query("""
                select
                aiwts.id
                from aruba_iap_wlan_traffic_stg aiwts
                where aiwts.mark=0
                order by aiwts.poll_time,aiwts.id
                limit 10000
                for update
                """,
                new BeanPropertyRowMapper<>(ArubaAiWlanTrafficEntity.class)
            );

        if (unprocessedList.isEmpty()) {
            log.info("no aruba ai wlan traffic record found");
            return;
        }

        String unprocessedIds =
            unprocessedList.stream()
            .map(ArubaAiWlanTrafficEntity::getId)
            .map(String::valueOf)
            .collect(Collectors.joining(",","(",")"));

        List<ArubaAiWlanTrafficEntity> processedList = jdbcTemplate
            .query(String.format("""
                with cte as(
                    select
                    distinct
                    date(aiwts.poll_time),
                    time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00')),
                    aiwts.wlan_mac,
                    aiwts.wlan_essid
                    from aruba_iap_wlan_traffic_stg aiwts
                    where aiwts.id in %s
                )
                select
                id
                from (
                    select
                    aiwts.id,
                    row_number() over(partition by date(aiwts.poll_time),time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00')),aiwts.wlan_mac,aiwts.wlan_essid 
                    order by aiwts.poll_time desc,aiwts.id desc) as rn
                    from (
                        select
                        *
                        from aruba_iap_wlan_traffic_stg aiwts
                        where mark=1
                        order by id desc
                        limit 100 -- limit to 100 because this table is small ~ last 2 hrs
                        for share
                    ) aiwts
                    where (date(aiwts.poll_time),time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00')),aiwts.wlan_mac,aiwts.wlan_essid)
                    in (select * from cte)
                ) x
                where x.rn=1
                """,
                unprocessedIds),
                new BeanPropertyRowMapper<>(ArubaAiWlanTrafficEntity.class)
            );

        String unprocessedAndProcessedIds = Stream.concat(processedList.stream(), unprocessedList.stream())
            .map(ArubaAiWlanTrafficEntity::getId)
            .map(String::valueOf)
            .collect(Collectors.joining(",","(",")"));

        log.info("unprocessed ids: {}", unprocessedIds);

        log.info("unprocessedAndProcessedIds: {}", unprocessedAndProcessedIds);

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
                    and aiwts.id in %s
                )""",
                unprocessedIds
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
                    and aiwts.id in %s
                )""",
                unprocessedIds
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
                    where dd.date_key is null and aiwts.id in %s
                );""",
                unprocessedIds
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
                    where td.time_key is null and aiwts.id in %s
                )""",
                unprocessedIds)
        );

        // insert hour into time dim
        jdbcTemplate.execute(
            String.format("""
                insert ignore into time_dim(time) (
                    select
                    distinct time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00'))
                    from aruba_iap_wlan_traffic_stg aiwts
                    left join time_dim td on time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00'))=td.time
                    where td.time_key is null and aiwts.id in %s
                )""",
            unprocessedIds)
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
                                lag(aiwts.wlan_tx,1,aiwts.wlan_tx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time,aiwts.id)
                                >
                                aiwts.wlan_tx,
                                0,
                                lag(aiwts.wlan_tx,1,aiwts.wlan_tx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time,aiwts.id)
                            )
                            +
                            aiwts.wlan_rx - if(
                                lag(aiwts.wlan_rx,1,aiwts.wlan_rx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time,aiwts.id)
                                >
                                aiwts.wlan_rx,
                                0,
                                lag(aiwts.wlan_rx,1,aiwts.wlan_rx) over(partition by dd.date_key,td.time_key,gid.iface_key order by aiwts.poll_time,aiwts.id)
                            )
                            as transmission_bytes
                            from aruba_iap_wlan_traffic_stg aiwts
                            join date_dim dd on date(aiwts.poll_time)=dd.date
                            join time_dim td on time_to_sec(date_format(time(aiwts.poll_time),'%%H:00:00'))=td.time
                            join gw_iface_dim gid on aiwts.wlan_mac=gid.iface_mac and aiwts.wlan_essid=gid.iface_name
                            where aiwts.id in %s
                        ) x
                        group by 1,2,3
                    ) tmp
                )
                on duplicate key update transmission_bytes=transmission_bytes+transmission_bytes_val""",
                unprocessedAndProcessedIds
            )
        );

        log.info("update fact table completed");

        log.info("mark records start");

        // once completed, mark all completed
        jdbcTemplate.execute(
            String.format("""
                update aruba_iap_wlan_traffic_stg set mark=1 where id in %s""",
                unprocessedIds
            )
        );

        log.info("mark records completed");

        log.info("end");
    }
}
